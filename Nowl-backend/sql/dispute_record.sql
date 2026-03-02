-- 纠纷仲裁记录表
-- 用于处理商品交易和跑腿服务的纠纷

CREATE TABLE IF NOT EXISTS `dispute_record` (
    `record_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '仲裁记录ID（主键）',
    `initiator_id` BIGINT NOT NULL COMMENT '发起人ID',
    `related_id` BIGINT NOT NULL COMMENT '关联人ID（被投诉方）',
    `content_id` BIGINT NOT NULL COMMENT '关联订单/跑腿任务ID',
    `target_type` TINYINT NOT NULL COMMENT '争议类型：0-商品交易，1-跑腿劳务',
    `content` TEXT NOT NULL COMMENT '争议内容',
    `evidence_urls` TEXT COMMENT '证据URL（JSON格式，多张图片）',
    `handle_status` TINYINT DEFAULT 0 COMMENT '处理状态：0-待处理，1-处理中，2-已解决，3-已驳回，4-已撤回',
    `handle_result` TEXT DEFAULT NULL COMMENT '处理结果',
    `claim_seller_credit_penalty` TINYINT NOT NULL DEFAULT 0 COMMENT '申请扣除卖家信用分：0-否，1-是',
    `claim_refund` TINYINT NOT NULL DEFAULT 0 COMMENT '申请退还金额：0-否，1-是',
    `claim_refund_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '申请退还金额',
    `initiator_reply_count` INT NOT NULL DEFAULT 0 COMMENT '发起人补充次数',
    `related_reply_count` INT NOT NULL DEFAULT 0 COMMENT '被投诉方补充次数',
    `conversation_logs` TEXT COMMENT '双方交流记录(JSON)',
    `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID（管理员）',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`record_id`),
    KEY `idx_initiator_id` (`initiator_id`) COMMENT '发起人索引',
    KEY `idx_related_id` (`related_id`) COMMENT '被投诉人索引',
    KEY `idx_content_target` (`content_id`, `target_type`) COMMENT '关联ID+类型，定位争议来源',
    KEY `idx_handle_status` (`handle_status`) COMMENT '处理状态索引，筛选待处理纠纷'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='纠纷仲裁记录表';
