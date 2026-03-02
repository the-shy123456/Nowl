-- 评价记录表
-- 用于存储商品交易和跑腿任务的评价信息

CREATE TABLE IF NOT EXISTS `review_record` (
    `review_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID（主键）',
    `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID（商品交易时填写）',
    `task_id` BIGINT DEFAULT NULL COMMENT '关联跑腿任务ID（跑腿时填写）',
    `target_type` TINYINT NOT NULL COMMENT '类型：0-商品交易，1-跑腿任务',
    `reviewer_id` BIGINT NOT NULL COMMENT '评价人ID',
    `reviewed_id` BIGINT NOT NULL COMMENT '被评价人ID',
    `rating` TINYINT NOT NULL COMMENT '评分：1-5星',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    `anonymous` TINYINT DEFAULT 0 COMMENT '是否匿名：0-否，1-是',
    `credit_change` INT DEFAULT 0 COMMENT '信用分变化值',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`review_id`),
    KEY `idx_reviewer_id` (`reviewer_id`) COMMENT '评价人索引',
    KEY `idx_reviewed_id` (`reviewed_id`) COMMENT '被评价人索引',
    KEY `idx_order_id` (`order_id`) COMMENT '订单索引',
    KEY `idx_task_id` (`task_id`) COMMENT '跑腿任务索引',
    KEY `idx_target_type` (`target_type`) COMMENT '类型索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价记录表';
