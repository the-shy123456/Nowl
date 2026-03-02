-- 用户聊天消息表
CREATE TABLE IF NOT EXISTS `chat_message` (
    `message_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
    `school_code` VARCHAR(16) DEFAULT NULL COMMENT '消息所属学校(可选冗余)',
    `campus_code` VARCHAR(16) DEFAULT NULL COMMENT '消息所属校区(可选冗余)',
    `content` TEXT COMMENT '消息内容',
    `message_type` INT NOT NULL DEFAULT 0 COMMENT '消息类型：0-文本，1-图片',
    `is_read` INT NOT NULL DEFAULT 0 COMMENT '已读状态：0-未读，1-已读',
    `risk_level` VARCHAR(16) DEFAULT 'low' COMMENT '风控等级',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`message_id`),
    KEY `idx_chat_sender_receiver_time` (`sender_id`, `receiver_id`, `create_time`),
    KEY `idx_chat_receiver_read` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户聊天消息表';
