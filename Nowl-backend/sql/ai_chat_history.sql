-- AI聊天历史记录表
CREATE TABLE IF NOT EXISTS `ai_chat_history` (
    `message_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` VARCHAR(10) NOT NULL COMMENT '角色：user-用户, model-AI',
    `content` TEXT COMMENT '消息内容',
    `image_url` VARCHAR(500) COMMENT '图片URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`message_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天历史记录表';
