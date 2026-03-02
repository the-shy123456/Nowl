package com.unimarket.module.dispute.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.module.dispute.entity.DisputeRecord;
import com.unimarket.module.dispute.mapper.DisputeRecordMapper;
import com.unimarket.common.enums.*;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.result.PageResult;
import com.unimarket.module.dispute.dto.DisputeCreateDTO;
import com.unimarket.module.dispute.dto.DisputeReplyDTO;
import com.unimarket.module.dispute.service.DisputePermissionService;
import com.unimarket.module.dispute.service.DisputeService;
import com.unimarket.module.dispute.vo.DisputeDetailVO;
import com.unimarket.module.dispute.vo.DisputeConversationItemVO;
import com.unimarket.module.dispute.vo.DisputeListItemVO;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.order.entity.OrderInfo;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 纠纷服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {

    private static final int MAX_CONVERSATION_COUNT_PER_SIDE = 3;

    private final DisputeRecordMapper disputeRecordMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final ErrandTaskMapper errandTaskMapper;
    private final GoodsInfoMapper goodsInfoMapper;
    private final UserInfoMapper userInfoMapper;
    private final NoticeService noticeService;
    private final DisputePermissionService disputePermissionService;
    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDispute(Long userId, DisputeCreateDTO dto) {
        String lockKey = "dispute:create:" + dto.getTargetType() + ":" + dto.getContentId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            doCreateDispute(userId, dto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("发起纠纷过程中断");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void doCreateDispute(Long userId, DisputeCreateDTO dto) {
        Long relatedId;
        String contentTitle;
        String schoolCode;
        String campusCode;

        int claimSellerCreditPenalty = normalizeBinaryClaim(dto.getClaimSellerCreditPenalty(), "申请扣除卖家信用分");
        int claimRefund = normalizeBinaryClaim(dto.getClaimRefund(), "申请退还金额");
        if (claimRefund == 0) {
            dto.setClaimRefundAmount(null);
        } else if (dto.getClaimRefundAmount() == null || dto.getClaimRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("申请退款金额必须大于0");
        }

        // 根据类型验证并获取关联信息
        if (DisputeTargetType.ORDER.getCode().equals(dto.getTargetType())) {
            // 商品交易纠纷
            OrderInfo order = orderInfoMapper.selectById(dto.getContentId());
            if (order == null) {
                throw new BusinessException("订单不存在");
            }
            // 验证用户是否是订单参与方
            if (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId())) {
                throw new BusinessException("您无权对此订单发起纠纷");
            }
            boolean disputeWindowOpen = OrderStatus.PENDING_RECEIVE.getCode().equals(order.getOrderStatus());
            if (!disputeWindowOpen) {
                throw new BusinessException("当前订单状态不可发起纠纷，仅支持待确认收货订单");
            }
            if (RefundStatus.PENDING.getCode().equals(order.getRefundStatus())) {
                throw new BusinessException("订单退款处理中，暂不可发起纠纷");
            }
            // 确定被投诉方
            relatedId = userId.equals(order.getBuyerId()) ? order.getSellerId() : order.getBuyerId();

            // 获取商品标题
            GoodsInfo goods = goodsInfoMapper.selectById(order.getProductId());
            contentTitle = goods != null ? goods.getTitle() : "商品订单";
            schoolCode = order.getSchoolCode();
            campusCode = order.getCampusCode();

            if (claimRefund == 1) {
                if (dto.getClaimRefundAmount() == null || dto.getClaimRefundAmount().compareTo(order.getTotalAmount()) > 0) {
                    throw new BusinessException("申请退款金额不能为空且不能超过订单支付金额");
                }
            }
        } else if (DisputeTargetType.ERRAND.getCode().equals(dto.getTargetType())) {
            // 跑腿劳务纠纷
            ErrandTask task = errandTaskMapper.selectById(dto.getContentId());
            if (task == null) {
                throw new BusinessException("跑腿任务不存在");
            }
            // 验证用户是否是任务参与方
            if (!userId.equals(task.getPublisherId()) && !userId.equals(task.getAcceptorId())) {
                throw new BusinessException("您无权对此任务发起纠纷");
            }
            // 确定被投诉方
            relatedId = userId.equals(task.getPublisherId()) ? task.getAcceptorId() : task.getPublisherId();
            if (relatedId == null) {
                throw new BusinessException("该任务尚未被接单，无法发起纠纷");
            }
            contentTitle = task.getTitle();
            schoolCode = task.getSchoolCode();
            campusCode = task.getCampusCode();

            if (claimRefund == 1
                    && (dto.getClaimRefundAmount() == null || dto.getClaimRefundAmount().compareTo(task.getReward()) > 0)) {
                throw new BusinessException("申请退款金额不能为空且不能超过跑腿酬金");
            }
        } else {
            throw new BusinessException("无效的争议类型");
        }

        // 检查是否已存在未处理的纠纷
        LambdaQueryWrapper<DisputeRecord> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(DisputeRecord::getContentId, dto.getContentId())
                .eq(DisputeRecord::getTargetType, dto.getTargetType())
                .in(DisputeRecord::getHandleStatus, DisputeStatus.PENDING.getCode(), DisputeStatus.PROCESSING.getCode()); // 待处理或处理中
        Long existCount = disputeRecordMapper.selectCount(checkWrapper);
        if (existCount > 0) {
            throw new BusinessException("该订单/任务已存在进行中的纠纷，请勿重复发起");
        }

        // 创建纠纷记录
        DisputeRecord record = new DisputeRecord();
        record.setInitiatorId(userId);
        record.setRelatedId(relatedId);
        record.setContentId(dto.getContentId());
        record.setTargetType(dto.getTargetType());
        record.setSchoolCode(schoolCode);
        record.setCampusCode(campusCode);
        record.setContent(dto.getContent());
        record.setEvidenceUrls(dto.getEvidenceUrls());
        record.setHandleStatus(DisputeStatus.PENDING.getCode()); // 待处理
        record.setClaimSellerCreditPenalty(claimSellerCreditPenalty);
        record.setClaimRefund(claimRefund);
        record.setClaimRefundAmount(dto.getClaimRefundAmount());
        record.setInitiatorReplyCount(0);
        record.setRelatedReplyCount(0);
        record.setConversationLogs(JSONUtil.toJsonStr(new ArrayList<>()));

        disputeRecordMapper.insert(record);

        log.info("用户{}发起纠纷，记录ID：{}，类型：{}", userId, record.getRecordId(), dto.getTargetType());

        // 发送通知给被投诉方
        String typeDesc = DisputeTargetType.ORDER.getCode().equals(dto.getTargetType()) ? "商品交易" : "跑腿任务";
        noticeService.sendNotice(relatedId, "收到纠纷投诉",
                "您的" + typeDesc + " [" + contentTitle + "] 收到了一份纠纷投诉，请关注处理进展。",
                NoticeType.DISPUTE.getCode(), record.getRecordId());
    }

    @Override
    public PageResult<DisputeListItemVO> getMyDisputes(Long userId, Integer pageNum, Integer pageSize, Integer handleStatus) {
        LambdaQueryWrapper<DisputeRecord> wrapper = new LambdaQueryWrapper<>();
        // 查询我发起的或与我相关的纠纷
        wrapper.and(w -> w.eq(DisputeRecord::getInitiatorId, userId)
                .or()
                .eq(DisputeRecord::getRelatedId, userId));

        if (handleStatus != null) {
            wrapper.eq(DisputeRecord::getHandleStatus, handleStatus);
        }
        wrapper.orderByDesc(DisputeRecord::getCreateTime);

        Page<DisputeRecord> page = new Page<>(pageNum, pageSize);
        Page<DisputeRecord> result = disputeRecordMapper.selectPage(page, wrapper);

        List<DisputeListItemVO> voList = convertToListItemVO(result.getRecords(), userId);

        return new PageResult<>(result.getTotal(), voList);
    }

    @Override
    public DisputeDetailVO getDisputeDetail(Long userId, Long recordId) {
        DisputeRecord record = disputeRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("纠纷记录不存在");
        }

        // 验证权限（参与方 + 具备纠纷查看权限的管理员）
        if (!disputePermissionService.canView(userId, record)) {
            throw new BusinessException("您无权查看此纠纷详情");
        }

        return convertToDetailVO(record, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawDispute(Long userId, Long recordId) {
        if (!disputePermissionService.canWithdraw(userId, recordId)) {
            throw new BusinessException("无法撤回此纠纷");
        }

        DisputeRecord record = disputeRecordMapper.selectById(recordId);
        record.setHandleStatus(DisputeStatus.WITHDRAWN.getCode()); // 已撤回
        disputeRecordMapper.updateById(record);

        log.info("用户{}撤回纠纷，记录ID：{}", userId, recordId);

        // 通知被投诉方
        noticeService.sendNotice(record.getRelatedId(), "纠纷已撤回",
                "针对您的纠纷投诉已被发起人撤回。", NoticeType.DISPUTE.getCode(), recordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addEvidence(Long userId, DisputeReplyDTO dto) {
        String lockKey = "dispute:reply:" + dto.getRecordId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            doAddEvidence(userId, dto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("补充证据过程中断");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void doAddEvidence(Long userId, DisputeReplyDTO dto) {
        if (!disputePermissionService.isParticipant(userId, dto.getRecordId())) {
            throw new BusinessException("您无权补充此纠纷的证据");
        }

        DisputeRecord record = disputeRecordMapper.selectById(dto.getRecordId());
        if (record == null) {
            throw new BusinessException("纠纷记录不存在");
        }

        // 只能在待处理或处理中状态补充证据
        if (!DisputeStatus.PENDING.getCode().equals(record.getHandleStatus()) && 
            !DisputeStatus.PROCESSING.getCode().equals(record.getHandleStatus())) {
            throw new BusinessException("当前状态不允许补充证据");
        }

        boolean isInitiator = userId.equals(record.getInitiatorId());
        int currentReplyCount = isInitiator
                ? (record.getInitiatorReplyCount() == null ? 0 : record.getInitiatorReplyCount())
                : (record.getRelatedReplyCount() == null ? 0 : record.getRelatedReplyCount());
        if (currentReplyCount >= MAX_CONVERSATION_COUNT_PER_SIDE) {
            throw new BusinessException("每方最多只能补充3次");
        }

        boolean hasContent = StrUtil.isNotBlank(dto.getAdditionalContent());
        boolean hasEvidence = StrUtil.isNotBlank(dto.getAdditionalEvidence());
        if (!hasContent && !hasEvidence) {
            throw new BusinessException("请至少补充文字说明或证据");
        }
        if (hasContent) {
            dto.setAdditionalContent(dto.getAdditionalContent().trim());
        }

        List<String> replyEvidence = new ArrayList<>();
        if (hasEvidence) {
            try {
                replyEvidence = JSONUtil.toList(dto.getAdditionalEvidence(), String.class);
            } catch (Exception ex) {
                throw new BusinessException("补充证据格式错误");
            }
            if (replyEvidence.size() > 9) {
                throw new BusinessException("单次补充证据最多9张");
            }
        }

        List<JSONObject> conversationLogs = parseConversationLogs(record.getConversationLogs());
        JSONObject newReply = new JSONObject();
        newReply.set("userId", userId);
        newReply.set("initiator", isInitiator);
        newReply.set("content", dto.getAdditionalContent());
        newReply.set("evidenceUrls", replyEvidence);
        newReply.set("createTime", java.time.LocalDateTime.now().toString());
        conversationLogs.add(newReply);
        record.setConversationLogs(JSONUtil.toJsonStr(conversationLogs));

        if (isInitiator) {
            record.setInitiatorReplyCount(currentReplyCount + 1);
        } else {
            record.setRelatedReplyCount(currentReplyCount + 1);
        }

        if (DisputeStatus.PENDING.getCode().equals(record.getHandleStatus())) {
            record.setHandleStatus(DisputeStatus.PROCESSING.getCode());
        }

        disputeRecordMapper.updateById(record);

        log.info("用户{}补充纠纷证据，记录ID：{}", userId, dto.getRecordId());
    }

    /**
     * 转换为列表项VO
     */
    private List<DisputeListItemVO> convertToListItemVO(List<DisputeRecord> records, Long currentUserId) {
        if (records.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询用户信息
        List<Long> userIds = records.stream()
                .flatMap(r -> List.of(r.getInitiatorId(), r.getRelatedId()).stream())
                .distinct()
                .collect(Collectors.toList());
        List<UserInfo> users = userInfoMapper.selectBatchIds(userIds);
        Map<Long, UserInfo> userMap = users.stream()
                .collect(Collectors.toMap(UserInfo::getUserId, u -> u));

        // 批量查询订单和跑腿信息
        List<Long> orderIds = records.stream()
                .filter(r -> r.getTargetType() == 0)
                .map(DisputeRecord::getContentId)
                .collect(Collectors.toList());
        Map<Long, OrderInfo> orderMap = orderIds.isEmpty() ? Map.of() :
                orderInfoMapper.selectBatchIds(orderIds).stream()
                        .collect(Collectors.toMap(OrderInfo::getOrderId, o -> o));

        List<Long> errandIds = records.stream()
                .filter(r -> r.getTargetType() == 1)
                .map(DisputeRecord::getContentId)
                .collect(Collectors.toList());
        Map<Long, ErrandTask> errandMap = errandIds.isEmpty() ? Map.of() :
                errandTaskMapper.selectBatchIds(errandIds).stream()
                        .collect(Collectors.toMap(ErrandTask::getTaskId, e -> e));

        // 批量查询商品信息
        List<Long> productIds = orderMap.values().stream()
                .map(OrderInfo::getProductId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, GoodsInfo> goodsMap = productIds.isEmpty() ? Map.of() :
                goodsInfoMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(GoodsInfo::getProductId, g -> g));

        List<DisputeListItemVO> voList = new ArrayList<>();
        for (DisputeRecord record : records) {
            DisputeListItemVO vo = new DisputeListItemVO();
            vo.setRecordId(record.getRecordId());
            vo.setTargetType(record.getTargetType());
            vo.setTargetTypeDesc(DisputeTargetType.ORDER.getCode().equals(record.getTargetType()) ? "商品交易" : "跑腿劳务");
            vo.setContentId(record.getContentId());
            vo.setHandleStatus(record.getHandleStatus());
            vo.setStatusDesc(DisputeStatus.getDescriptionByCode(record.getHandleStatus()));
            vo.setCreateTime(record.getCreateTime());
            vo.setUpdateTime(record.getUpdateTime());

            // 内容摘要
            String content = record.getContent();
            vo.setContentSummary(content.length() > 50 ? content.substring(0, 50) + "..." : content);

            // 判断是否是发起人
            boolean isInitiator = currentUserId.equals(record.getInitiatorId());
            vo.setIsInitiator(isInitiator);

            // 对方用户信息
            Long otherUserId = isInitiator ? record.getRelatedId() : record.getInitiatorId();
            vo.setOtherUserId(otherUserId);
            UserInfo otherUser = userMap.get(otherUserId);
            if (otherUser != null) {
                vo.setOtherUserName(otherUser.getNickName());
                vo.setOtherUserAvatar(otherUser.getImageUrl());
            }

            // 关联内容信息
            if (DisputeTargetType.ORDER.getCode().equals(record.getTargetType())) {
                OrderInfo order = orderMap.get(record.getContentId());
                if (order != null) {
                    GoodsInfo goods = goodsMap.get(order.getProductId());
                    if (goods != null) {
                        vo.setContentTitle(goods.getTitle());
                        vo.setContentImage(goods.getImage());
                    }
                }
            } else {
                ErrandTask task = errandMap.get(record.getContentId());
                if (task != null) {
                    vo.setContentTitle(task.getTitle());
                    // 跑腿任务第一张图片
                    if (StrUtil.isNotBlank(task.getImageList())) {
                        List<String> images = JSONUtil.toList(task.getImageList(), String.class);
                        if (!images.isEmpty()) {
                            vo.setContentImage(images.get(0));
                        }
                    }
                }
            }

            voList.add(vo);
        }

        return voList;
    }

    /**
     * 转换为详情VO
     */
    private DisputeDetailVO convertToDetailVO(DisputeRecord record, Long currentUserId) {
        DisputeDetailVO vo = new DisputeDetailVO();
        BeanUtil.copyProperties(record, vo);

        vo.setTargetTypeDesc(DisputeTargetType.ORDER.getCode().equals(record.getTargetType()) ? "商品交易" : "跑腿劳务");
        vo.setStatusDesc(DisputeStatus.getDescriptionByCode(record.getHandleStatus()));

        // 解析证据URL列表
        if (StrUtil.isNotBlank(record.getEvidenceUrls())) {
            vo.setEvidenceUrlList(JSONUtil.toList(record.getEvidenceUrls(), String.class));
        } else {
            vo.setEvidenceUrlList(new ArrayList<>());
        }

        // 判断是否是发起人和是否可撤回
        boolean isParticipant = currentUserId.equals(record.getInitiatorId()) || currentUserId.equals(record.getRelatedId());
        boolean isInitiator = currentUserId.equals(record.getInitiatorId());
        vo.setIsInitiator(isInitiator);
        vo.setCanWithdraw(isParticipant && disputePermissionService.canWithdraw(currentUserId, record.getRecordId()));
        int selfReplyCount = isInitiator
                ? (record.getInitiatorReplyCount() == null ? 0 : record.getInitiatorReplyCount())
                : (record.getRelatedReplyCount() == null ? 0 : record.getRelatedReplyCount());
        boolean canReply = isParticipant && (DisputeStatus.PENDING.getCode().equals(record.getHandleStatus())
                || DisputeStatus.PROCESSING.getCode().equals(record.getHandleStatus()))
                && selfReplyCount < MAX_CONVERSATION_COUNT_PER_SIDE;
        vo.setCanReply(canReply);

        // 发起人信息
        UserInfo initiator = userInfoMapper.selectById(record.getInitiatorId());
        if (initiator != null) {
            vo.setInitiatorId(initiator.getUserId());
            vo.setInitiatorName(initiator.getNickName());
            vo.setInitiatorAvatar(initiator.getImageUrl());
        }

        // 被投诉方信息
        UserInfo related = userInfoMapper.selectById(record.getRelatedId());
        if (related != null) {
            vo.setRelatedId(related.getUserId());
            vo.setRelatedName(related.getNickName());
            vo.setRelatedAvatar(related.getImageUrl());
        }

        vo.setConversations(buildConversationVOList(record.getConversationLogs(), initiator, related));

        // 关联内容信息
        if (DisputeTargetType.ORDER.getCode().equals(record.getTargetType())) {
            // 订单信息
            OrderInfo order = orderInfoMapper.selectById(record.getContentId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setOrderAmount(order.getTotalAmount());
                vo.setOrderStatus(order.getOrderStatus());

                GoodsInfo goods = goodsInfoMapper.selectById(order.getProductId());
                if (goods != null) {
                    vo.setProductTitle(goods.getTitle());
                    vo.setProductImage(goods.getImage());
                }
            }
        } else {
            // 跑腿任务信息
            ErrandTask task = errandTaskMapper.selectById(record.getContentId());
            if (task != null) {
                vo.setErrandTitle(task.getTitle());
                vo.setErrandReward(task.getReward());
                vo.setErrandStatus(task.getTaskStatus());

                if (StrUtil.isNotBlank(task.getImageList())) {
                    List<String> images = JSONUtil.toList(task.getImageList(), String.class);
                    if (!images.isEmpty()) {
                        vo.setErrandImage(images.get(0));
                    }
                }
            }
        }

        return vo;
    }


    private int normalizeBinaryClaim(Integer value, String fieldName) {
        if (value == null) {
            return 0;
        }
        if (value != 0 && value != 1) {
            throw new BusinessException(fieldName + "参数非法");
        }
        return value;
    }

    private List<JSONObject> parseConversationLogs(String rawConversationLogs) {
        if (StrUtil.isBlank(rawConversationLogs)) {
            return new ArrayList<>();
        }
        JSONArray jsonArray = JSONUtil.parseArray(rawConversationLogs);
        List<JSONObject> list = new ArrayList<>();
        for (Object item : jsonArray) {
            list.add(JSONUtil.parseObj(item));
        }
        return list;
    }

    private List<DisputeConversationItemVO> buildConversationVOList(
            String rawConversationLogs,
            UserInfo initiator,
            UserInfo related
    ) {
        List<JSONObject> logs = parseConversationLogs(rawConversationLogs);
        List<DisputeConversationItemVO> result = new ArrayList<>();
        for (JSONObject logItem : logs) {
            DisputeConversationItemVO itemVO = new DisputeConversationItemVO();
            Long userId = logItem.getLong("userId");
            Boolean isInitiator = logItem.getBool("initiator", false);
            itemVO.setUserId(userId);
            itemVO.setInitiator(isInitiator);
            itemVO.setContent(logItem.getStr("content"));

            JSONArray evidenceArray = logItem.getJSONArray("evidenceUrls");
            List<String> evidenceUrls = new ArrayList<>();
            if (evidenceArray != null) {
                for (Object obj : evidenceArray) {
                    evidenceUrls.add(String.valueOf(obj));
                }
            }
            itemVO.setEvidenceUrls(evidenceUrls);

            String timeText = logItem.getStr("createTime");
            if (StrUtil.isNotBlank(timeText)) {
                itemVO.setCreateTime(java.time.LocalDateTime.parse(timeText));
            }

            if (Boolean.TRUE.equals(isInitiator) && initiator != null) {
                itemVO.setUserName(initiator.getNickName());
                itemVO.setUserAvatar(initiator.getImageUrl());
            } else if (related != null) {
                itemVO.setUserName(related.getNickName());
                itemVO.setUserAvatar(related.getImageUrl());
            }

            result.add(itemVO);
        }
        return result;
    }
}
