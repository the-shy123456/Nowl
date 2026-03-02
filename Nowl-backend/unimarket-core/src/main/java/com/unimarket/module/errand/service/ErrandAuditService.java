package com.unimarket.module.errand.service;

import com.unimarket.module.errand.dto.ErrandAuditResult;
import com.unimarket.module.errand.dto.ErrandPublishDTO;

/**
 * 跑腿任务审核服务
 */
public interface ErrandAuditService {

    /**
     * 审核跑腿发布内容（文本+图片）
     *
     * @param dto 发布内容
     * @return 审核结果
     */
    ErrandAuditResult audit(ErrandPublishDTO dto);
}
