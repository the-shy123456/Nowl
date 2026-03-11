package com.unimarket.module.goods.service.impl;

import cn.hutool.json.JSONUtil;
import com.unimarket.ai.dto.AiAuditResult;
import com.unimarket.ai.service.AiAuditService;
import com.unimarket.common.config.RocketMQConfig;
import com.unimarket.common.enums.ReviewStatus;
import com.unimarket.common.enums.TradeStatus;
import com.unimarket.common.mq.GoodsAuditMessage;
import com.unimarket.common.mq.GoodsSyncMessage;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.goods.service.GoodsAuditService;
import com.unimarket.module.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 商品审核服务实现
 * 由 MQ 消费者调用，不再使用 @Async
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsAuditServiceImpl implements GoodsAuditService {

    private final GoodsInfoMapper goodsInfoMapper;
    private final AiAuditService aiAuditService;
    private final NoticeService noticeService;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void performAudit(Long goodsId, int operationType) {
        log.info("开始审核商品: goodsId={}, operationType={}", goodsId, operationType);
        GoodsInfo goods = goodsInfoMapper.selectById(goodsId);
        if (goods == null) {
            log.warn("审核商品不存在: {}", goodsId);
            return;
        }

        String timeStr = goods.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // 1. 文本审核
        String content = goods.getTitle() + " " + goods.getDescription();
        AiAuditResult textResult = aiAuditService.auditText(content);

        String riskLevel = textResult.getRiskLevel();
        if ("high".equals(riskLevel) || !textResult.isSafe()) {
            handleAuditFailure(goods, textResult.getReason(), timeStr);
            return;
        }

        if ("medium".equals(riskLevel)) {
            handlePendingHumanReview(goods, textResult.getReason(), timeStr);
            return;
        }

        // 2. 图片审核
        if (goods.getImage() != null) {
            AiAuditResult imageResult = aiAuditService.auditImage(goods.getImage());
            if ("high".equals(imageResult.getRiskLevel()) || !imageResult.isSafe()) {
                String reason = (imageResult.getReason() == null || imageResult.getReason().isBlank())
                    ? "商品图片存在高风险，需人工复核"
                    : imageResult.getReason();
                handlePendingHumanReview(goods, reason, timeStr);
                return;
            }
            if ("medium".equals(imageResult.getRiskLevel())) {
                String reason = (imageResult.getReason() == null || imageResult.getReason().isBlank())
                    ? "商品图片需要人工复核"
                    : imageResult.getReason();
                handlePendingHumanReview(goods, reason, timeStr);
                return;
            }
        }
        if (goods.getImageList() != null) {
            try {
                List<String> images = JSONUtil.toList(goods.getImageList(), String.class);
                for (String img : images) {
                    AiAuditResult imageResult = aiAuditService.auditImage(img);
                    if ("high".equals(imageResult.getRiskLevel()) || !imageResult.isSafe()) {
                        String reason = (imageResult.getReason() == null || imageResult.getReason().isBlank())
                            ? "商品详情图片存在高风险，需人工复核"
                            : imageResult.getReason();
                        handlePendingHumanReview(goods, reason, timeStr);
                        return;
                    }
                    if ("medium".equals(imageResult.getRiskLevel())) {
                        String reason = (imageResult.getReason() == null || imageResult.getReason().isBlank())
                            ? "商品详情图片需要人工复核"
                            : imageResult.getReason();
                        handlePendingHumanReview(goods, reason, timeStr);
                        return;
                    }
                }
            } catch (Exception e) {
                log.warn("解析详情图列表失败: {}", goods.getImageList());
            }
        }

        // 3. 审核通过
        goods.setReviewStatus(ReviewStatus.AI_PASSED.getCode());
        goods.setAuditReason(null);
        // 通过审核后才对外上架；若已售出则保持售出状态
        if (!TradeStatus.SOLD.getCode().equals(goods.getTradeStatus())) {
            goods.setTradeStatus(TradeStatus.ON_SALE.getCode());
        }
        goodsInfoMapper.updateById(goods);
        noticeService.sendNotice(
            goods.getSellerId(),
            "商品审核通过",
            String.format("亲爱的用户，关于您在%s发布的【%s】审核成功，点击查看。", timeStr, goods.getTitle()),
            0,
            goodsId
        );
        log.info("商品审核通过: {}", goodsId);

        // 4. 发送 ES 同步消息
        sendSyncMessage(goodsId, operationType);
    }

    /**
     * 发送 ES 同步消息
     */
    private void sendSyncMessage(Long goodsId, int operationType) {
        GoodsSyncMessage message = operationType == GoodsAuditMessage.TYPE_CREATE
                ? GoodsSyncMessage.createMessage(goodsId)
                : GoodsSyncMessage.updateMessage(goodsId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSendSyncMessage(goodsId, message);
                }
            });
            return;
        }
        doSendSyncMessage(goodsId, message);
    }

    private void doSendSyncMessage(Long goodsId, GoodsSyncMessage message) {
        try {
            rocketMQTemplate.convertAndSend(RocketMQConfig.GOODS_SYNC_TOPIC, message);
            log.info("发送商品同步消息成功: goodsId={}", goodsId);
        } catch (Exception e) {
            log.error("发送商品同步消息失败: goodsId={}", goodsId, e);
        }
    }

    private void handlePendingHumanReview(GoodsInfo goods, String reason, String timeStr) {
        goods.setReviewStatus(ReviewStatus.WAIT_MANUAL.getCode());
        goods.setAuditReason(reason);
        // 待人工复核期间不对外上架；若已售出则保持售出状态
        if (!TradeStatus.SOLD.getCode().equals(goods.getTradeStatus())) {
            goods.setTradeStatus(TradeStatus.OFF_SHELF.getCode());
        }
        goodsInfoMapper.updateById(goods);
        noticeService.sendNotice(
            goods.getSellerId(),
            "商品待人工复核",
            String.format("亲爱的用户，关于您在%s发布的【%s】需要进一步审核，请耐心等待。", timeStr, goods.getTitle()),
            0,
            goods.getProductId()
        );
        log.info("商品待人工复核: {}, 原因: {}", goods.getProductId(), reason);
    }

    private void handleAuditFailure(GoodsInfo goods, String reason, String timeStr) {
        goods.setReviewStatus(ReviewStatus.REJECTED.getCode());
        goods.setAuditReason(reason);
        // 审核失败不对外上架；若已售出则保持售出状态
        if (!TradeStatus.SOLD.getCode().equals(goods.getTradeStatus())) {
            goods.setTradeStatus(TradeStatus.OFF_SHELF.getCode());
        }
        goodsInfoMapper.updateById(goods);
        noticeService.sendNotice(
            goods.getSellerId(),
            "商品审核失败",
            String.format("亲爱的用户，关于您在%s发布的【%s】审核失败。原因：%s。点击查看。", timeStr, goods.getTitle(), reason),
            0,
            goods.getProductId()
        );
        log.info("商品审核失败: {}, 原因: {}", goods.getProductId(), reason);
    }
}

