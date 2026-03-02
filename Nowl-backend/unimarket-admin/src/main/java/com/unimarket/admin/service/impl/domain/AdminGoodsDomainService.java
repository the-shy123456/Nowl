package com.unimarket.admin.service.impl.domain;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.admin.service.impl.support.AdminActionLockSupport;
import com.unimarket.admin.service.impl.support.AdminScopeSupport;
import com.unimarket.admin.service.impl.support.AdminSchoolInfoSupport;
import com.unimarket.common.config.RocketMQConfig;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.common.enums.ReviewStatus;
import com.unimarket.common.enums.TradeStatus;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.mq.GoodsSyncMessage;
import com.unimarket.common.result.PageQuery;
import com.unimarket.common.result.ResultCode;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.goods.vo.GoodsVO;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.school.entity.SchoolInfo;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGoodsDomainService {

    private final GoodsInfoMapper goodsInfoMapper;
    private final UserInfoMapper userInfoMapper;
    private final NoticeService noticeService;
    private final IamAccessService iamAccessService;
    private final RocketMQTemplate rocketMQTemplate;
    private final AdminActionLockSupport actionLockSupport;
    private final AdminScopeSupport scopeSupport;
    private final AdminSchoolInfoSupport schoolInfoSupport;

    @Transactional(rollbackFor = Exception.class)
    public void auditGoods(Long operatorId, Long goodsId, Integer status, String reason) {
        String lockKey = "admin:audit:goods:" + goodsId;
        actionLockSupport.withLock(lockKey, () -> doAuditGoods(operatorId, goodsId, status, reason));
    }

    private void doAuditGoods(Long operatorId, Long goodsId, Integer status, String reason) {
        GoodsInfo goods = goodsInfoMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        iamAccessService.assertCanManageScope(operatorId, goods.getSchoolCode(), goods.getCampusCode());

        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException("审核状态仅支持 1-通过 或 2-驳回");
        }
        Integer currentReviewStatus = goods.getReviewStatus();
        Integer targetReviewStatus = status == 1
                ? ReviewStatus.MANUAL_PASSED.getCode()
                : ReviewStatus.REJECTED.getCode();
        if (targetReviewStatus.equals(currentReviewStatus)) {
            log.info("商品审核重复请求已忽略: goodsId={}, reviewStatus={}", goodsId, currentReviewStatus);
            return;
        }
        if (ReviewStatus.MANUAL_PASSED.getCode().equals(currentReviewStatus)
                || ReviewStatus.REJECTED.getCode().equals(currentReviewStatus)) {
            throw new BusinessException("商品已被其他管理员处理，请刷新后重试");
        }
        if (!ReviewStatus.WAIT_MANUAL.getCode().equals(goods.getReviewStatus())
                && !ReviewStatus.WAIT_REVIEW.getCode().equals(goods.getReviewStatus())) {
            throw new BusinessException("当前商品状态不可复核");
        }

        boolean sold = TradeStatus.SOLD.getCode().equals(goods.getTradeStatus());

        if (status == 1) {
            goods.setReviewStatus(ReviewStatus.MANUAL_PASSED.getCode());
            goods.setAuditReason(null);
            if (!sold) {
                goods.setTradeStatus(TradeStatus.ON_SALE.getCode());
            }
            goodsInfoMapper.updateById(goods);

            noticeService.sendNotice(
                    goods.getSellerId(),
                    "商品审核通过",
                    "您的商品【" + goods.getTitle() + "】已通过人工复核并恢复可见。",
                    NoticeType.SYSTEM.getCode(),
                    goodsId
            );

            sendGoodsSyncAfterCommit(GoodsSyncMessage.updateMessage(goodsId));
        } else {
            String rejectReason = StrUtil.isBlank(reason) ? "商品内容未通过平台审核规范" : reason;
            goods.setReviewStatus(ReviewStatus.REJECTED.getCode());
            goods.setAuditReason(rejectReason);
            if (!sold) {
                goods.setTradeStatus(TradeStatus.OFF_SHELF.getCode());
            }
            goodsInfoMapper.updateById(goods);

            noticeService.sendNotice(
                    goods.getSellerId(),
                    "商品审核未通过",
                    "您的商品【" + goods.getTitle() + "】未通过人工复核。原因：" + rejectReason,
                    NoticeType.SYSTEM.getCode(),
                    goodsId
            );

            sendGoodsSyncAfterCommit(GoodsSyncMessage.deleteMessage(goodsId));
        }
        log.info("商品审核完成: goodsId={}, status={}, reason={}", goodsId, status, reason);
    }

    @Transactional(rollbackFor = Exception.class)
    public void forceOfflineGoods(Long operatorId, Long goodsId, String reason) {
        GoodsInfo goods = goodsInfoMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException("商品不存在");
        }
        iamAccessService.assertCanManageScope(operatorId, goods.getSchoolCode(), goods.getCampusCode());
        goods.setTradeStatus(TradeStatus.OFF_SHELF.getCode()); // 下架
        goods.setReviewStatus(ReviewStatus.REJECTED.getCode()); // 违规
        goodsInfoMapper.updateById(goods);

        // 发送通知给卖家
        noticeService.sendNotice(
                goods.getSellerId(),
                "商品下架通知",
                "您的商品 [" + goods.getTitle() + "] 因违规已被管理员强制下架。原因：" + reason,
                NoticeType.TRADE.getCode(),
                goodsId
        );

        log.info("商品强制下架: goodsId={}, reason={}", goodsId, reason);
    }

    public Page<GoodsVO> getPendingAuditGoods(Long operatorId,
                                              PageQuery query,
                                              String schoolCode,
                                              String campusCode,
                                              Integer reviewStatus) {
        List<IamAdminScopeBinding> scopes = scopeSupport.getOperatorScopes(operatorId);

        Page<GoodsInfo> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<>();
        if (reviewStatus != null) {
            wrapper.eq(GoodsInfo::getReviewStatus, reviewStatus);
        } else {
            // 默认展示“待审核 + 待人工复核”
            wrapper.in(GoodsInfo::getReviewStatus, ReviewStatus.WAIT_REVIEW.getCode(), ReviewStatus.WAIT_MANUAL.getCode());
        }
        wrapper.eq(StrUtil.isNotBlank(schoolCode), GoodsInfo::getSchoolCode, schoolCode)
                .eq(StrUtil.isNotBlank(campusCode), GoodsInfo::getCampusCode, campusCode)
                .orderByDesc(GoodsInfo::getCreateTime);
        scopeSupport.applyScopeFilter(wrapper, scopes, GoodsInfo::getSchoolCode, GoodsInfo::getCampusCode);

        Page<GoodsInfo> goodsPage = goodsInfoMapper.selectPage(page, wrapper);
        List<GoodsInfo> records = goodsPage.getRecords();
        if (records.isEmpty()) {
            return new Page<GoodsVO>(goodsPage.getCurrent(), goodsPage.getSize(), goodsPage.getTotal());
        }

        Set<Long> sellerIds = records.stream().map(GoodsInfo::getSellerId).collect(Collectors.toSet());
        Map<Long, UserInfo> userMap = userInfoMapper.selectBatchIds(sellerIds).stream()
                .collect(Collectors.toMap(UserInfo::getUserId, u -> u));
        Map<String, SchoolInfo> schoolMap = schoolInfoSupport.buildSchoolInfoMap(records, GoodsInfo::getSchoolCode);

        List<GoodsVO> vos = records.stream().map(g -> {
            GoodsVO vo = BeanUtil.copyProperties(g, GoodsVO.class);
            UserInfo seller = userMap.get(g.getSellerId());
            if (seller != null) {
                vo.setSellerName(seller.getNickName());
                vo.setSellerAvatar(seller.getImageUrl());
                vo.setSellerAuthStatus(seller.getAuthStatus());
            }
            schoolInfoSupport.fillSchoolCampusNames(
                    g.getSchoolCode(),
                    g.getCampusCode(),
                    schoolMap,
                    vo::setSchoolName,
                    vo::setCampusName
            );
            return vo;
        }).collect(Collectors.toList());

        return new Page<GoodsVO>(goodsPage.getCurrent(), goodsPage.getSize(), goodsPage.getTotal()).setRecords(vos);
    }

    private void sendGoodsSyncAfterCommit(GoodsSyncMessage message) {
        if (message == null) {
            return;
        }
        Runnable syncTask = () -> {
            try {
                rocketMQTemplate.convertAndSend(RocketMQConfig.GOODS_SYNC_TOPIC, message);
            } catch (Exception e) {
                log.error("发送商品同步消息失败: type={}, goodsId={}", message.getType(), message.getProductId(), e);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            syncTask.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                syncTask.run();
            }
        });
    }

    public Page<GoodsVO> getAllGoods(Long operatorId,
                                     PageQuery query,
                                     String keyword,
                                     String schoolCode,
                                     String campusCode,
                                     Integer tradeStatus,
                                     Integer reviewStatus) {
        List<IamAdminScopeBinding> scopes = scopeSupport.getOperatorScopes(operatorId);

        Page<GoodsInfo> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(GoodsInfo::getTitle, keyword);
        }
        wrapper.eq(StrUtil.isNotBlank(schoolCode), GoodsInfo::getSchoolCode, schoolCode)
                .eq(StrUtil.isNotBlank(campusCode), GoodsInfo::getCampusCode, campusCode)
                .eq(tradeStatus != null, GoodsInfo::getTradeStatus, tradeStatus)
                .eq(reviewStatus != null, GoodsInfo::getReviewStatus, reviewStatus);
        wrapper.orderByDesc(GoodsInfo::getCreateTime);
        scopeSupport.applyScopeFilter(wrapper, scopes, GoodsInfo::getSchoolCode, GoodsInfo::getCampusCode);

        Page<GoodsInfo> goodsPage = goodsInfoMapper.selectPage(page, wrapper);
        List<GoodsInfo> records = goodsPage.getRecords();

        if (records.isEmpty()) {
            return new Page<GoodsVO>(goodsPage.getCurrent(), goodsPage.getSize(), goodsPage.getTotal());
        }

        // 批量查询卖家信息
        Set<Long> sellerIds = records.stream().map(GoodsInfo::getSellerId).collect(Collectors.toSet());
        Map<Long, UserInfo> userMap = userInfoMapper.selectBatchIds(sellerIds).stream()
                .collect(Collectors.toMap(UserInfo::getUserId, u -> u));
        Map<String, SchoolInfo> schoolMap = schoolInfoSupport.buildSchoolInfoMap(records, GoodsInfo::getSchoolCode);

        List<GoodsVO> vos = records.stream()
                .map(g -> {
                    GoodsVO vo = BeanUtil.copyProperties(g, GoodsVO.class);
                    UserInfo seller = userMap.get(g.getSellerId());
                    if (seller != null) {
                        vo.setSellerName(seller.getNickName());
                        vo.setSellerAvatar(seller.getImageUrl());
                        vo.setSellerAuthStatus(seller.getAuthStatus());
                    }
                    schoolInfoSupport.fillSchoolCampusNames(
                            g.getSchoolCode(),
                            g.getCampusCode(),
                            schoolMap,
                            vo::setSchoolName,
                            vo::setCampusName
                    );
                    return vo;
                })
                .collect(Collectors.toList());

        return new Page<GoodsVO>(goodsPage.getCurrent(), goodsPage.getSize(), goodsPage.getTotal()).setRecords(vos);
    }
}
