package com.unimarket.module.errand.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.unimarket.ai.dto.AiAuditResult;
import com.unimarket.ai.service.AiAuditService;
import com.unimarket.common.enums.ReviewStatus;
import com.unimarket.module.errand.dto.ErrandAuditResult;
import com.unimarket.module.errand.dto.ErrandPublishDTO;
import com.unimarket.module.errand.service.ErrandAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 跑腿任务审核服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrandAuditServiceImpl implements ErrandAuditService {

    private final AiAuditService aiAuditService;

    @Override
    public ErrandAuditResult audit(ErrandPublishDTO dto) {
        // 1. 文本审核
        AiAuditResult textResult = aiAuditService.auditText(buildAuditText(dto));
        if (isHighRisk(textResult)) {
            String reason = defaultReason(textResult.getReason(), "跑腿文本内容存在高风险，审核未通过");
            return new ErrandAuditResult(ReviewStatus.REJECTED.getCode(), reason);
        }
        if (isMediumRisk(textResult)) {
            String reason = defaultReason(textResult.getReason(), "跑腿文本内容需要人工复核");
            return new ErrandAuditResult(ReviewStatus.WAIT_MANUAL.getCode(), reason);
        }

        // 2. 图片审核：为降低误杀，图片高风险也进入人工复核
        List<String> imageUrls = parseImages(dto.getImageList());
        if (imageUrls == null) {
            return new ErrandAuditResult(ReviewStatus.WAIT_MANUAL.getCode(), "跑腿图片数据格式异常，需人工复核");
        }
        for (String imageUrl : imageUrls) {
            AiAuditResult imageResult = aiAuditService.auditImage(imageUrl);
            if (isHighRisk(imageResult)) {
                String reason = defaultReason(imageResult.getReason(), "跑腿图片存在高风险，需人工复核");
                return new ErrandAuditResult(ReviewStatus.WAIT_MANUAL.getCode(), reason);
            }
            if (isMediumRisk(imageResult)) {
                String reason = defaultReason(imageResult.getReason(), "跑腿图片识别不确定，需人工复核");
                return new ErrandAuditResult(ReviewStatus.WAIT_MANUAL.getCode(), reason);
            }
        }

        return new ErrandAuditResult(ReviewStatus.AI_PASSED.getCode(), null);
    }

    private String buildAuditText(ErrandPublishDTO dto) {
        StringBuilder builder = new StringBuilder();
        append(builder, dto.getTitle());
        append(builder, dto.getDescription());
        append(builder, dto.getTaskContent());
        append(builder, dto.getPickupAddress());
        append(builder, dto.getDeliveryAddress());
        append(builder, dto.getRemark());
        return builder.toString().trim();
    }

    private void append(StringBuilder builder, String value) {
        if (StrUtil.isNotBlank(value)) {
            builder.append(value).append('\n');
        }
    }

    private List<String> parseImages(String imageList) {
        if (StrUtil.isBlank(imageList)) {
            return List.of();
        }
        try {
            List<String> images = JSONUtil.toList(imageList, String.class);
            List<String> result = new ArrayList<>();
            for (String image : images) {
                if (StrUtil.isNotBlank(image)) {
                    result.add(image);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("解析跑腿图片列表失败: {}", imageList);
            return null;
        }
    }

    private boolean isHighRisk(AiAuditResult result) {
        return "high".equals(result.getRiskLevel()) || !result.isSafe();
    }

    private boolean isMediumRisk(AiAuditResult result) {
        return "medium".equals(result.getRiskLevel());
    }

    private String defaultReason(String reason, String fallback) {
        return StrUtil.isBlank(reason) ? fallback : reason;
    }
}
