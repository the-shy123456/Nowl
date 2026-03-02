package com.unimarket.module.review.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.common.enums.CreditLevel;
import com.unimarket.common.enums.ErrandStatus;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.common.enums.OrderStatus;
import com.unimarket.common.enums.ReviewRating;
import com.unimarket.common.enums.ReviewTargetType;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.result.PageResult;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.order.entity.OrderInfo;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import com.unimarket.module.review.dto.ReviewCreateDTO;
import com.unimarket.module.review.entity.ReviewRecord;
import com.unimarket.module.review.mapper.ReviewRecordMapper;
import com.unimarket.module.review.service.ReviewService;
import com.unimarket.module.review.vo.ReviewListItemVO;
import com.unimarket.module.review.vo.UserReviewStatsVO;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.module.user.service.CreditScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评价服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRecordMapper reviewRecordMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final ErrandTaskMapper errandTaskMapper;
    private final GoodsInfoMapper goodsInfoMapper;
    private final UserInfoMapper userInfoMapper;
    private final CreditScoreService creditScoreService;
    private final NoticeService noticeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReview(Long userId, ReviewCreateDTO dto) {
        Long contentId = isGoodsTarget(dto.getTargetType()) ? dto.getOrderId() : dto.getTaskId();

        // 1. 验证是否可以评价
        if (!canReview(userId, dto.getTargetType(), contentId, dto.getReviewedId())) {
            throw new BusinessException("无法对此交易进行评价");
        }

        // 2. 检查是否已评价
        if (hasReviewed(dto.getOrderId(), dto.getTaskId(), dto.getTargetType(), userId)) {
            throw new BusinessException("您已对此交易进行过评价");
        }

        // 3. 差评需要填写理由
        if (ReviewRating.isBadReview(dto.getRating())) {
            if (StrUtil.isBlank(dto.getContent()) || dto.getContent().length() < 10) {
                throw new BusinessException("差评需要填写至少10字的评价理由");
            }
        }

        // 4. 计算信用分变化
        int creditChange = ReviewRating.getCreditChangeByCode(dto.getRating());

        // 5. 创建评价记录
        ReviewRecord record = new ReviewRecord();
        record.setOrderId(dto.getOrderId());
        record.setTaskId(dto.getTaskId());
        record.setTargetType(dto.getTargetType());
        record.setReviewerId(userId);
        record.setReviewedId(dto.getReviewedId());
        record.setRating(dto.getRating());
        record.setContent(dto.getContent());
        record.setAnonymous(dto.getAnonymous() != null ? dto.getAnonymous() : 0);
        record.setCreditChange(creditChange);

        reviewRecordMapper.insert(record);

        // 6. 调整被评价人信用分
        if (creditChange != 0) {
            creditScoreService.adjustByReview(dto.getReviewedId(), dto.getRating());
        }

        log.info("用户{}评价用户{}，评分：{}，信用分变化：{}", userId, dto.getReviewedId(), dto.getRating(), creditChange);

        // 7. 发送通知
        String ratingDesc = dto.getRating() >= 4 ? "好评" : (dto.getRating() <= 2 ? "差评" : "评价");
        noticeService.sendNotice(dto.getReviewedId(), "收到新评价",
                "您收到了一条" + ratingDesc + "，快去查看吧！",
                NoticeType.REVIEW.getCode());
    }

    @Override
    public PageResult<ReviewListItemVO> getReceivedReviews(Long userId, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null ? 1 : Math.max(pageNum, 1);
        int safePageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<ReviewRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewRecord::getReviewedId, userId)
                .orderByDesc(ReviewRecord::getCreateTime);

        Page<ReviewRecord> page = new Page<>(safePageNum, safePageSize);
        Page<ReviewRecord> result = reviewRecordMapper.selectPage(page, wrapper);

        List<ReviewListItemVO> voList = convertToVOList(result.getRecords());
        return new PageResult<>(result.getTotal(), voList);
    }

    @Override
    public PageResult<ReviewListItemVO> getSentReviews(Long userId, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null ? 1 : Math.max(pageNum, 1);
        int safePageSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<ReviewRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewRecord::getReviewerId, userId)
                .orderByDesc(ReviewRecord::getCreateTime);

        Page<ReviewRecord> page = new Page<>(safePageNum, safePageSize);
        Page<ReviewRecord> result = reviewRecordMapper.selectPage(page, wrapper);

        List<ReviewListItemVO> voList = convertToVOList(result.getRecords());
        return new PageResult<>(result.getTotal(), voList);
    }

    @Override
    public UserReviewStatsVO getUserReviewStats(Long userId) {
        UserReviewStatsVO stats = new UserReviewStatsVO();
        stats.setUserId(userId);

        // 获取平均评分
        Double avgRating = reviewRecordMapper.getAverageRating(userId);
        stats.setAverageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 5.0);

        // 获取评价数量
        Long totalReviews = reviewRecordMapper.countReviewsReceived(userId);
        stats.setTotalReviews(totalReviews != null ? totalReviews : 0L);

        // 获取好评数量
        Long goodReviews = reviewRecordMapper.countGoodReviews(userId);
        stats.setGoodReviews(goodReviews != null ? goodReviews : 0L);

        // 计算好评率
        if (totalReviews != null && totalReviews > 0) {
            stats.setGoodRate(Math.round(goodReviews * 1000.0 / totalReviews) / 10.0);
        } else {
            stats.setGoodRate(100.0);
        }

        // 获取信用分和等级
        int creditScore = creditScoreService.getCreditScore(userId);
        stats.setCreditScore(creditScore);
        stats.setCreditLevel(CreditLevel.getDescriptionByScore(creditScore));
        stats.setCreditColor(CreditLevel.getColorByScore(creditScore));

        return stats;
    }

    @Override
    public boolean canReview(Long userId, Integer targetType, Long contentId, Long reviewedId) {
        if (isGoodsTarget(targetType)) {
            // 商品交易：检查订单状态和参与方
            OrderInfo order = orderInfoMapper.selectById(contentId);
            if (order == null) {
                return false;
            }
            // 订单必须已完成
            if (!OrderStatus.COMPLETED.getCode().equals(order.getOrderStatus())) {
                return false;
            }
            // 必须是订单参与方
            if (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId())) {
                return false;
            }
            // 被评价人必须是订单另一方
            if (userId.equals(order.getBuyerId())) {
                return reviewedId.equals(order.getSellerId());
            } else {
                return reviewedId.equals(order.getBuyerId());
            }
        } else if (isErrandTarget(targetType)) {
            // 跑腿任务：检查任务状态和参与方
            ErrandTask task = errandTaskMapper.selectById(contentId);
            if (task == null) {
                return false;
            }
            // 任务必须已完成
            if (!ErrandStatus.COMPLETED.getCode().equals(task.getTaskStatus())) {
                return false;
            }
            // 必须是任务参与方
            if (!userId.equals(task.getPublisherId()) && !userId.equals(task.getAcceptorId())) {
                return false;
            }
            // 被评价人必须是任务另一方
            if (userId.equals(task.getPublisherId())) {
                return reviewedId.equals(task.getAcceptorId());
            } else {
                return reviewedId.equals(task.getPublisherId());
            }
        }
        return false;
    }

    @Override
    public boolean hasReviewed(Long orderId, Long taskId, Integer targetType, Long reviewerId) {
        LambdaQueryWrapper<ReviewRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewRecord::getReviewerId, reviewerId)
                .eq(ReviewRecord::getTargetType, targetType);

        if (isGoodsTarget(targetType) && orderId != null) {
            wrapper.eq(ReviewRecord::getOrderId, orderId);
        } else if (isErrandTarget(targetType) && taskId != null) {
            wrapper.eq(ReviewRecord::getTaskId, taskId);
        } else {
            return false;
        }

        return reviewRecordMapper.selectCount(wrapper) > 0;
    }

    /**
     * 转换为VO列表
     */
    private List<ReviewListItemVO> convertToVOList(List<ReviewRecord> records) {
        if (records.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询用户信息
        List<Long> userIds = records.stream()
                .map(ReviewRecord::getReviewerId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UserInfo> userMap = userInfoMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserInfo::getUserId, u -> u));

        // 批量查询订单和商品信息
        List<Long> orderIds = records.stream()
                .filter(r -> isGoodsTarget(r.getTargetType()) && r.getOrderId() != null)
                .map(ReviewRecord::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, OrderInfo> orderMap = orderIds.isEmpty() ? Map.of() :
                orderInfoMapper.selectBatchIds(orderIds).stream()
                        .collect(Collectors.toMap(OrderInfo::getOrderId, o -> o));

        List<Long> productIds = orderMap.values().stream()
                .map(OrderInfo::getProductId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, GoodsInfo> goodsMap = productIds.isEmpty() ? Map.of() :
                goodsInfoMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(GoodsInfo::getProductId, g -> g));

        // 批量查询跑腿任务信息
        List<Long> taskIds = records.stream()
                .filter(r -> isErrandTarget(r.getTargetType()) && r.getTaskId() != null)
                .map(ReviewRecord::getTaskId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, ErrandTask> taskMap = taskIds.isEmpty() ? Map.of() :
                errandTaskMapper.selectBatchIds(taskIds).stream()
                        .collect(Collectors.toMap(ErrandTask::getTaskId, t -> t));

        List<ReviewListItemVO> voList = new ArrayList<>();
        for (ReviewRecord record : records) {
            ReviewListItemVO vo = new ReviewListItemVO();
            vo.setReviewId(record.getReviewId());
            vo.setTargetType(record.getTargetType());
            vo.setTargetTypeDesc(getTargetTypeDesc(record.getTargetType()));
            vo.setOrderId(record.getOrderId());
            vo.setTaskId(record.getTaskId());
            vo.setReviewerId(record.getReviewerId());
            vo.setRating(record.getRating());
            vo.setContent(record.getContent());
            vo.setAnonymous(record.getAnonymous() == 1);
            vo.setCreateTime(record.getCreateTime());
            if (isGoodsTarget(record.getTargetType())) {
                vo.setReviewerRole("交易方");
                vo.setReviewedRole("交易方");
            } else if (isErrandTarget(record.getTargetType())) {
                vo.setReviewerRole("参与方");
                vo.setReviewedRole("参与方");
            }

            // 评价人信息（匿名则隐藏）
            if (record.getAnonymous() == 1) {
                vo.setReviewerName("匿名用户");
                vo.setReviewerAvatar(null);
            } else {
                UserInfo reviewer = userMap.get(record.getReviewerId());
                if (reviewer != null) {
                    vo.setReviewerName(reviewer.getNickName());
                    vo.setReviewerAvatar(reviewer.getImageUrl());
                }
            }

            // 关联内容信息
            if (isGoodsTarget(record.getTargetType()) && record.getOrderId() != null) {
                OrderInfo order = orderMap.get(record.getOrderId());
                if (order != null) {
                    vo.setProductId(order.getProductId());
                    vo.setReviewerRole(resolveGoodsRole(record.getReviewerId(), order));
                    vo.setReviewedRole(resolveGoodsRole(record.getReviewedId(), order));
                    GoodsInfo goods = goodsMap.get(order.getProductId());
                    if (goods != null) {
                        vo.setContentTitle(goods.getTitle());
                        vo.setContentImage(goods.getImage());
                    }
                }
            } else if (isErrandTarget(record.getTargetType()) && record.getTaskId() != null) {
                ErrandTask task = taskMap.get(record.getTaskId());
                if (task != null) {
                    vo.setReviewerRole(resolveErrandRole(record.getReviewerId(), task));
                    vo.setReviewedRole(resolveErrandRole(record.getReviewedId(), task));
                    vo.setContentTitle(task.getTitle());
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

    private String resolveGoodsRole(Long userId, OrderInfo order) {
        if (userId == null || order == null) {
            return "交易方";
        }
        if (userId.equals(order.getBuyerId())) {
            return "买家";
        }
        if (userId.equals(order.getSellerId())) {
            return "卖家";
        }
        return "交易方";
    }

    private String resolveErrandRole(Long userId, ErrandTask task) {
        if (userId == null || task == null) {
            return "参与方";
        }
        if (userId.equals(task.getPublisherId())) {
            return "发单者";
        }
        if (userId.equals(task.getAcceptorId())) {
            return "跑腿员";
        }
        return "参与方";
    }

    private boolean isGoodsTarget(Integer targetType) {
        return ReviewTargetType.GOODS_TRADE.getCode().equals(targetType);
    }

    private boolean isErrandTarget(Integer targetType) {
        return ReviewTargetType.ERRAND_SERVICE.getCode().equals(targetType);
    }

    private String getTargetTypeDesc(Integer targetType) {
        ReviewTargetType type = ReviewTargetType.getByCode(targetType);
        return type != null ? type.getDescription() : "未知类型";
    }
}
