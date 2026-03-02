-- 系统通知表
CREATE TABLE IF NOT EXISTS `sys_notice` (
  `notice_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint(20) NOT NULL COMMENT '接收用户ID',
  `title` varchar(100) NOT NULL COMMENT '通知标题',
  `content` varchar(500) DEFAULT NULL COMMENT '通知内容',
  `type` tinyint(4) DEFAULT '0' COMMENT '通知类型：0-系统通知, 1-订单通知, 2-跑腿通知',
  `related_id` bigint(20) DEFAULT NULL COMMENT '关联ID（如商品ID、订单ID等）',
  `is_read` tinyint(4) DEFAULT '0' COMMENT '是否已读：0-未读, 1-已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`notice_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';
