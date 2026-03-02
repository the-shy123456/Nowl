-- =====================================================
-- UniMarket New (unimarketnew) Full Schema
-- Version: v1.0.0 (Breaking, No Backward Compatibility)
-- =====================================================

CREATE DATABASE IF NOT EXISTS `unimarketnew` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `unimarketnew`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- 1) Base Domain
-- =========================

CREATE TABLE IF NOT EXISTS `school_info` (
  `school_code` VARCHAR(16) NOT NULL COMMENT '学校编码',
  `school_name` VARCHAR(128) NOT NULL COMMENT '学校名称',
  `campus_code` VARCHAR(16) NOT NULL COMMENT '校区编码',
  `campus_name` VARCHAR(128) NOT NULL COMMENT '校区名称',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用,1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`school_code`, `campus_code`),
  KEY `idx_school_status` (`school_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校/校区信息';

CREATE TABLE IF NOT EXISTS `user_info` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `phone` VARCHAR(25) NOT NULL COMMENT '手机号',
  `password` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `student_no` VARCHAR(32) DEFAULT NULL COMMENT '学号/工号',
  `nick_name` VARCHAR(25) NOT NULL COMMENT '昵称',
  `user_name` VARCHAR(25) DEFAULT NULL COMMENT '真实姓名',
  `image_url` VARCHAR(512) DEFAULT NULL COMMENT '头像',
  `cert_image` VARCHAR(512) DEFAULT NULL COMMENT '证件照',
  `self_image` VARCHAR(512) DEFAULT NULL COMMENT '本人照',
  `school_code` VARCHAR(16) DEFAULT NULL COMMENT '学校编码',
  `campus_code` VARCHAR(16) DEFAULT NULL COMMENT '校区编码',
  `auth_status` TINYINT NOT NULL DEFAULT 0 COMMENT '认证:0未认证,1待审,2通过,3拒绝',
  `runnable_status` TINYINT NOT NULL DEFAULT 0 COMMENT '跑腿员:0未申请,1待审,2通过,3拒绝',
  `account_status` TINYINT NOT NULL DEFAULT 0 COMMENT '账号:0正常,1封禁',
  `credit_score` INT NOT NULL DEFAULT 100 COMMENT '信用分',
  `user_type` TINYINT NOT NULL DEFAULT 0 COMMENT '用户类型',
  `gender` TINYINT NOT NULL DEFAULT 0 COMMENT '性别',
  `grade` VARCHAR(16) DEFAULT NULL COMMENT '年级',
  `money` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '余额',
  `follow_count` INT NOT NULL DEFAULT 0,
  `fan_count` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  UNIQUE KEY `uk_user_student_no` (`student_no`),
  KEY `idx_user_school_campus` (`school_code`, `campus_code`),
  KEY `idx_user_auth_status` (`auth_status`),
  KEY `idx_user_runnable_status` (`runnable_status`),
  KEY `idx_user_account_status` (`account_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息';

CREATE TABLE IF NOT EXISTS `user_follow` (
  `follow_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `followed_user_id` BIGINT NOT NULL,
  `follow_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `is_cancel` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`follow_id`),
  UNIQUE KEY `uk_user_followed` (`user_id`, `followed_user_id`),
  KEY `idx_followed_user_id` (`followed_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系';

CREATE TABLE IF NOT EXISTS `item_category` (
  `category_id` INT NOT NULL AUTO_INCREMENT,
  `category_name` VARCHAR(64) NOT NULL,
  `parent_id` INT NOT NULL DEFAULT 0,
  `sort` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`),
  KEY `idx_parent_sort` (`parent_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

CREATE TABLE IF NOT EXISTS `goods_info` (
  `product_id` BIGINT NOT NULL AUTO_INCREMENT,
  `seller_id` BIGINT NOT NULL,
  `category_id` INT NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `image` VARCHAR(512) DEFAULT NULL,
  `image_list` TEXT COMMENT 'JSON数组',
  `school_code` VARCHAR(16) NOT NULL,
  `campus_code` VARCHAR(16) NOT NULL,
  `trade_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0在售,1售出,2下架',
  `review_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待审,1AI通过,2人工通过,3违规',
  `audit_reason` VARCHAR(500) DEFAULT NULL,
  `item_condition` TINYINT NOT NULL DEFAULT 5,
  `trade_type` TINYINT NOT NULL DEFAULT 0,
  `delivery_fee` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
  `price` DECIMAL(10,2) NOT NULL,
  `original_price` DECIMAL(10,2) DEFAULT NULL,
  `ai_valuation` DECIMAL(10,2) DEFAULT NULL,
  `collect_count` INT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`product_id`),
  KEY `idx_goods_seller` (`seller_id`),
  KEY `idx_goods_school_campus` (`school_code`, `campus_code`),
  KEY `idx_goods_status` (`trade_status`, `review_status`),
  KEY `idx_goods_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息';

CREATE TABLE IF NOT EXISTS `collection_record` (
  `collection_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`collection_id`),
  UNIQUE KEY `uk_collection_user_product` (`user_id`, `product_id`),
  KEY `idx_collection_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品收藏';

CREATE TABLE IF NOT EXISTS `order_info` (
  `order_id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL,
  `buyer_id` BIGINT NOT NULL,
  `seller_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `school_code` VARCHAR(16) NOT NULL COMMENT '订单所属学校(冗余)',
  `campus_code` VARCHAR(16) NOT NULL COMMENT '订单所属校区(冗余)',
  `order_amount` DECIMAL(10,2) NOT NULL,
  `delivery_fee` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
  `total_amount` DECIMAL(10,2) NOT NULL,
  `order_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付,1待发货,2待收货,3完成,4取消',
  `pay_time` DATETIME DEFAULT NULL,
  `delivery_time` DATETIME DEFAULT NULL,
  `receive_time` DATETIME DEFAULT NULL,
  `cancel_time` DATETIME DEFAULT NULL,
  `remark` VARCHAR(255) DEFAULT NULL,
  `refund_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0无,1待处理,2已退款,3拒绝',
  `refund_reason` VARCHAR(255) DEFAULT NULL,
  `refund_amount` DECIMAL(10,2) DEFAULT NULL,
  `refund_apply_time` DATETIME DEFAULT NULL,
  `refund_deadline` DATETIME DEFAULT NULL,
  `refund_process_time` DATETIME DEFAULT NULL,
  `refund_processor_id` BIGINT DEFAULT NULL,
  `refund_process_remark` VARCHAR(255) DEFAULT NULL,
  `refund_fast_track` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_order_buyer` (`buyer_id`),
  KEY `idx_order_seller` (`seller_id`),
  KEY `idx_order_product` (`product_id`),
  KEY `idx_order_scope` (`school_code`, `campus_code`),
  KEY `idx_order_status` (`order_status`, `refund_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息';

CREATE TABLE IF NOT EXISTS `errand_task` (
  `task_id` BIGINT NOT NULL AUTO_INCREMENT,
  `publisher_id` BIGINT NOT NULL,
  `acceptor_id` BIGINT DEFAULT NULL,
  `title` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `task_content` VARCHAR(255) NOT NULL,
  `image_list` TEXT COMMENT 'JSON数组',
  `pickup_address` VARCHAR(255) DEFAULT NULL,
  `delivery_address` VARCHAR(255) DEFAULT NULL,
  `pickup_latitude` DECIMAL(10,8) DEFAULT NULL,
  `pickup_longitude` DECIMAL(11,8) DEFAULT NULL,
  `delivery_latitude` DECIMAL(10,8) DEFAULT NULL,
  `delivery_longitude` DECIMAL(11,8) DEFAULT NULL,
  `reward` DECIMAL(10,2) NOT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `deadline` DATETIME DEFAULT NULL,
  `task_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待接单,1进行中,2待确认,3完成,4取消',
  `review_status` TINYINT NOT NULL DEFAULT 1 COMMENT '0待审核,1AI通过,2人工通过,3违规驳回,4待人工复核',
  `audit_reason` VARCHAR(500) DEFAULT NULL COMMENT '审核原因（驳回/复核说明）',
  `school_code` VARCHAR(16) NOT NULL,
  `campus_code` VARCHAR(16) NOT NULL,
  `evidence_image` VARCHAR(512) DEFAULT NULL,
  `accept_time` DATETIME DEFAULT NULL,
  `deliver_time` DATETIME DEFAULT NULL,
  `confirm_time` DATETIME DEFAULT NULL,
  `cancel_time` DATETIME DEFAULT NULL,
  `cancel_reason` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_id`),
  KEY `idx_errand_publisher` (`publisher_id`),
  KEY `idx_errand_acceptor` (`acceptor_id`),
  KEY `idx_errand_scope` (`school_code`, `campus_code`),
  KEY `idx_errand_status` (`task_status`),
  KEY `idx_errand_review_status` (`review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跑腿任务';

CREATE TABLE IF NOT EXISTS `review_record` (
  `review_id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT DEFAULT NULL,
  `task_id` BIGINT DEFAULT NULL,
  `target_type` TINYINT NOT NULL COMMENT '0商品,1跑腿',
  `reviewer_id` BIGINT NOT NULL,
  `reviewed_id` BIGINT NOT NULL,
  `rating` TINYINT NOT NULL COMMENT '1-5星',
  `content` VARCHAR(500) DEFAULT NULL,
  `anonymous` TINYINT NOT NULL DEFAULT 0,
  `credit_change` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`review_id`),
  KEY `idx_review_reviewer` (`reviewer_id`),
  KEY `idx_review_reviewed` (`reviewed_id`),
  KEY `idx_review_target` (`target_type`, `order_id`, `task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价记录';

CREATE TABLE IF NOT EXISTS `dispute_record` (
  `record_id` BIGINT NOT NULL AUTO_INCREMENT,
  `initiator_id` BIGINT NOT NULL,
  `related_id` BIGINT NOT NULL,
  `content_id` BIGINT NOT NULL,
  `target_type` TINYINT NOT NULL COMMENT '0订单,1跑腿',
  `school_code` VARCHAR(16) NOT NULL,
  `campus_code` VARCHAR(16) NOT NULL,
  `content` TEXT NOT NULL,
  `evidence_urls` TEXT COMMENT 'JSON数组',
  `handle_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理,1处理中,2已解决,3驳回,4撤回',
  `handle_result` TEXT,
  `claim_seller_credit_penalty` TINYINT NOT NULL DEFAULT 0 COMMENT '0否,1是',
  `claim_refund` TINYINT NOT NULL DEFAULT 0 COMMENT '0否,1是',
  `claim_refund_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '申请退款金额',
  `initiator_reply_count` INT NOT NULL DEFAULT 0 COMMENT '发起方补充次数',
  `related_reply_count` INT NOT NULL DEFAULT 0 COMMENT '被投诉方补充次数',
  `conversation_logs` LONGTEXT COMMENT '双方补充记录(JSON)',
  `handler_id` BIGINT DEFAULT NULL,
  `handle_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`record_id`),
  KEY `idx_dispute_scope` (`school_code`, `campus_code`),
  KEY `idx_dispute_status` (`handle_status`),
  KEY `idx_dispute_initiator` (`initiator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='纠纷记录';

CREATE TABLE IF NOT EXISTS `sys_notice` (
  `notice_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `content` TEXT,
  `type` TINYINT NOT NULL DEFAULT 0,
  `related_id` BIGINT DEFAULT NULL,
  `is_read` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`notice_id`),
  KEY `idx_notice_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知';

CREATE TABLE IF NOT EXISTS `chat_message` (
  `message_id` BIGINT NOT NULL AUTO_INCREMENT,
  `sender_id` BIGINT NOT NULL,
  `receiver_id` BIGINT NOT NULL,
  `school_code` VARCHAR(16) DEFAULT NULL COMMENT '消息所属学校(可选冗余)',
  `campus_code` VARCHAR(16) DEFAULT NULL COMMENT '消息所属校区(可选冗余)',
  `content` TEXT,
  `message_type` INT NOT NULL DEFAULT 0 COMMENT '0文本,1图片',
  `is_read` INT NOT NULL DEFAULT 0,
  `risk_level` VARCHAR(16) DEFAULT 'low',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`),
  KEY `idx_chat_sender_receiver_time` (`sender_id`, `receiver_id`, `create_time`),
  KEY `idx_chat_receiver_read` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私聊消息';

CREATE TABLE IF NOT EXISTS `chat_block_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `blocked_user_id` BIGINT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_block_user_target` (`user_id`, `blocked_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天拉黑';

CREATE TABLE IF NOT EXISTS `ai_chat_history` (
  `message_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role` VARCHAR(20) NOT NULL COMMENT 'user/model/system',
  `content` TEXT,
  `image_url` VARCHAR(512) DEFAULT NULL,
  `risk_level` VARCHAR(16) DEFAULT 'low',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`),
  KEY `idx_ai_chat_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天历史';

CREATE TABLE IF NOT EXISTS `user_behavior_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `behavior_type` TINYINT NOT NULL COMMENT '1浏览,2收藏,3购买,4搜索',
  `product_id` BIGINT DEFAULT NULL,
  `category_id` INT DEFAULT NULL,
  `keyword` VARCHAR(255) DEFAULT NULL,
  `duration` INT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_behavior_user_type_time` (`user_id`, `behavior_type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行为日志';

CREATE TABLE IF NOT EXISTS `goods_similarity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `similar_product_id` BIGINT NOT NULL,
  `similarity_score` DECIMAL(6,4) NOT NULL,
  `similarity_type` TINYINT NOT NULL DEFAULT 1,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_similarity_pair` (`product_id`, `similar_product_id`, `similarity_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品相似度';

CREATE TABLE IF NOT EXISTS `user_preference` (
  `user_id` BIGINT NOT NULL,
  `category_scores` JSON DEFAULT NULL,
  `price_preference` JSON DEFAULT NULL,
  `behavior_count` JSON DEFAULT NULL,
  `last_active_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好画像';

-- =========================
-- 2) IAM / RBAC / DataScope
-- =========================

CREATE TABLE IF NOT EXISTS `iam_role` (
  `role_id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(64) NOT NULL,
  `role_name` VARCHAR(128) NOT NULL,
  `role_level` INT NOT NULL DEFAULT 100 COMMENT '数值越小权限越高',
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_iam_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IAM角色';

CREATE TABLE IF NOT EXISTS `iam_permission` (
  `permission_id` BIGINT NOT NULL AUTO_INCREMENT,
  `permission_code` VARCHAR(128) NOT NULL,
  `permission_name` VARCHAR(128) NOT NULL,
  `permission_group` VARCHAR(64) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_iam_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IAM权限点';

CREATE TABLE IF NOT EXISTS `iam_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `expired_time` DATETIME DEFAULT NULL COMMENT '为空表示永久',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_user_role` (`user_id`, `role_id`),
  KEY `idx_iam_user_role_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS `iam_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_role_permission` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';

CREATE TABLE IF NOT EXISTS `iam_admin_scope_binding` (
  `binding_id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '管理员用户ID',
  `scope_type` VARCHAR(16) NOT NULL COMMENT 'ALL/SCHOOL/CAMPUS',
  `school_code` VARCHAR(16) DEFAULT NULL,
  `campus_code` VARCHAR(16) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`binding_id`),
  KEY `idx_iam_admin_scope_user` (`user_id`),
  KEY `idx_iam_admin_scope` (`scope_type`, `school_code`, `campus_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员独立范围绑定';

-- =========================
-- 3) Risk Control
-- =========================

CREATE TABLE IF NOT EXISTS `risk_rule` (
  `rule_id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_code` VARCHAR(64) NOT NULL,
  `rule_name` VARCHAR(128) NOT NULL,
  `event_type` VARCHAR(64) NOT NULL COMMENT 'LOGIN/CHAT/GOODS_PUBLISH/...',
  `rule_type` VARCHAR(32) NOT NULL COMMENT 'THRESHOLD/BLACKLIST/ML/SCRIPT',
  `rule_config` JSON NOT NULL,
  `decision_action` VARCHAR(32) NOT NULL COMMENT 'ALLOW/REJECT/CHALLENGE/REVIEW/LIMIT',
  `priority` INT NOT NULL DEFAULT 100,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_risk_rule_code` (`rule_code`),
  KEY `idx_risk_rule_event_status` (`event_type`, `status`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控规则';

CREATE TABLE IF NOT EXISTS `risk_event` (
  `event_id` BIGINT NOT NULL AUTO_INCREMENT,
  `trace_id` VARCHAR(64) NOT NULL,
  `event_type` VARCHAR(64) NOT NULL,
  `subject_type` VARCHAR(32) NOT NULL COMMENT 'USER/IP/DEVICE/CONTENT',
  `subject_id` VARCHAR(128) NOT NULL,
  `school_code` VARCHAR(16) DEFAULT NULL,
  `campus_code` VARCHAR(16) DEFAULT NULL,
  `risk_features` JSON DEFAULT NULL,
  `raw_payload` JSON DEFAULT NULL,
  `event_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  KEY `idx_risk_event_trace` (`trace_id`),
  KEY `idx_risk_event_subject` (`subject_type`, `subject_id`),
  KEY `idx_risk_event_type_time` (`event_type`, `event_time`),
  KEY `idx_risk_event_scope` (`school_code`, `campus_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控事件';

CREATE TABLE IF NOT EXISTS `risk_decision` (
  `decision_id` BIGINT NOT NULL AUTO_INCREMENT,
  `event_id` BIGINT NOT NULL,
  `decision_action` VARCHAR(32) NOT NULL COMMENT 'ALLOW/REJECT/CHALLENGE/REVIEW/LIMIT',
  `risk_level` VARCHAR(16) NOT NULL DEFAULT 'low',
  `risk_score` DECIMAL(8,2) DEFAULT NULL,
  `matched_rule_codes` JSON DEFAULT NULL,
  `decision_reason` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`decision_id`),
  KEY `idx_risk_decision_event` (`event_id`),
  KEY `idx_risk_decision_level` (`risk_level`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控决策';

CREATE TABLE IF NOT EXISTS `risk_case` (
  `case_id` BIGINT NOT NULL AUTO_INCREMENT,
  `event_id` BIGINT NOT NULL,
  `decision_id` BIGINT DEFAULT NULL,
  `school_code` VARCHAR(16) DEFAULT NULL,
  `campus_code` VARCHAR(16) DEFAULT NULL,
  `case_status` VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/PROCESSING/CLOSED/REJECTED',
  `assignee_id` BIGINT DEFAULT NULL,
  `result` VARCHAR(32) DEFAULT NULL COMMENT 'PASS/BLOCK/WARN',
  `result_reason` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`case_id`),
  KEY `idx_risk_case_status` (`case_status`, `create_time`),
  KEY `idx_risk_case_assignee` (`assignee_id`),
  KEY `idx_risk_case_scope` (`school_code`, `campus_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控工单';

CREATE TABLE IF NOT EXISTS `risk_blacklist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `subject_type` VARCHAR(32) NOT NULL COMMENT 'USER/IP/DEVICE/CONTENT',
  `subject_id` VARCHAR(128) NOT NULL,
  `reason` VARCHAR(255) DEFAULT NULL,
  `source` VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  `expire_time` DATETIME DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_risk_blacklist` (`subject_type`, `subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控黑名单';

CREATE TABLE IF NOT EXISTS `risk_whitelist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `subject_type` VARCHAR(32) NOT NULL,
  `subject_id` VARCHAR(128) NOT NULL,
  `reason` VARCHAR(255) DEFAULT NULL,
  `expire_time` DATETIME DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_risk_whitelist` (`subject_type`, `subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控白名单';

CREATE TABLE IF NOT EXISTS `risk_behavior_control` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `event_type` VARCHAR(64) NOT NULL COMMENT 'LOGIN/CHAT_SEND/.../ALL',
  `control_action` VARCHAR(32) NOT NULL COMMENT 'ALLOW/REJECT/REVIEW/LIMIT/CHALLENGE',
  `reason` VARCHAR(255) DEFAULT NULL,
  `expire_time` DATETIME DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `operator_id` BIGINT DEFAULT NULL COMMENT '后台操作人',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_risk_behavior_user_event` (`user_id`, `event_type`),
  KEY `idx_risk_behavior_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为风控管控';

-- =========================
-- 4) Audit
-- =========================

CREATE TABLE IF NOT EXISTS `audit_admin_operation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `trace_id` VARCHAR(64) NOT NULL,
  `operator_id` BIGINT NOT NULL,
  `operator_ip` VARCHAR(64) DEFAULT NULL,
  `module` VARCHAR(64) NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `target_type` VARCHAR(32) DEFAULT NULL,
  `target_id` VARCHAR(128) DEFAULT NULL,
  `request_payload` JSON DEFAULT NULL,
  `result_status` VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
  `result_message` VARCHAR(255) DEFAULT NULL,
  `cost_ms` INT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_admin_operator_time` (`operator_id`, `create_time`),
  KEY `idx_audit_admin_trace` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台操作审计';

CREATE TABLE IF NOT EXISTS `audit_permission_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `trace_id` VARCHAR(64) NOT NULL,
  `operator_id` BIGINT NOT NULL,
  `change_type` VARCHAR(32) NOT NULL COMMENT 'ROLE_GRANT/ROLE_REVOKE/PERM_GRANT/...',
  `target_user_id` BIGINT DEFAULT NULL,
  `target_role_id` BIGINT DEFAULT NULL,
  `target_permission_id` BIGINT DEFAULT NULL,
  `before_data` JSON DEFAULT NULL,
  `after_data` JSON DEFAULT NULL,
  `reason` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_perm_target_user` (`target_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限变更审计';

CREATE TABLE IF NOT EXISTS `audit_login_trace` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `trace_id` VARCHAR(64) NOT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `phone` VARCHAR(25) DEFAULT NULL,
  `ip` VARCHAR(64) DEFAULT NULL,
  `device_id` VARCHAR(128) DEFAULT NULL,
  `geo` VARCHAR(128) DEFAULT NULL,
  `login_result` VARCHAR(16) NOT NULL COMMENT 'SUCCESS/FAIL/CHALLENGE',
  `fail_reason` VARCHAR(255) DEFAULT NULL,
  `risk_level` VARCHAR(16) DEFAULT 'low',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_login_user_time` (`user_id`, `create_time`),
  KEY `idx_audit_login_ip_time` (`ip`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录审计轨迹';

-- =========================
-- 5) Seed Data
-- =========================

INSERT INTO `iam_role` (`role_code`, `role_name`, `role_level`) VALUES
  ('SUPER_ADMIN', '平台超级管理员', 1),
  ('SCHOOL_ADMIN', '学校管理员', 20),
  ('CAMPUS_ADMIN', '校区管理员', 30),
  ('CONTENT_AUDITOR', '内容审核员', 40),
  ('RISK_OPERATOR', '风控运营', 50),
  ('CUSTOMER_SUPPORT', '客服专员', 60),
  ('FINANCE_AUDITOR', '财务审核员', 70)
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `role_level` = VALUES(`role_level`),
  `status` = 1;

INSERT INTO `iam_permission` (`permission_code`, `permission_name`, `permission_group`) VALUES
  ('admin:dashboard:view', '查看管理看板', 'admin'),
  ('admin:goods:pending:view', '查看待审核商品', 'goods'),
  ('admin:goods:list:view', '查看商品列表', 'goods'),
  ('admin:goods:audit', '审核商品', 'goods'),
  ('admin:goods:offline', '下架商品', 'goods'),
  ('admin:user:list:view', '查看用户列表', 'user'),
  ('admin:user:status:update', '修改用户状态', 'user'),
  ('admin:auth:pending:view', '查看待认证用户', 'auth'),
  ('admin:auth:audit', '审核实名认证', 'auth'),
  ('admin:runner:pending:view', '查看待审核跑腿员', 'runner'),
  ('admin:runner:audit', '审核跑腿员', 'runner'),
  ('admin:order:list:view', '查看订单列表', 'order'),
  ('admin:dispute:list:view', '查看纠纷列表', 'dispute'),
  ('admin:dispute:handle', '处理纠纷', 'dispute'),
  ('admin:errand:list:view', '查看跑腿列表', 'errand'),
  ('admin:errand:audit', '复核跑腿任务', 'errand'),
  ('admin:iam:role:view', '查看IAM角色', 'iam'),
  ('admin:iam:user-role:view', '查看用户角色绑定', 'iam'),
  ('admin:iam:user-role:manage', '管理用户角色绑定', 'iam'),
  ('admin:iam:scope:view', '查看管理员范围绑定', 'iam'),
  ('admin:iam:scope:manage', '管理管理员范围绑定', 'iam'),
  ('admin:audit:operation:view', '查看后台操作审计', 'audit'),
  ('admin:audit:permission:view', '查看权限变更审计', 'audit'),
  ('admin:audit:login:view', '查看登录轨迹审计', 'audit'),
  ('risk:rule:manage', '管理风控规则', 'risk'),
  ('risk:event:view', '查看风控事件', 'risk'),
  ('risk:case:handle', '处理风控工单', 'risk'),
  ('admin:risk:behavior:view', '查看用户行为管控', 'risk'),
  ('admin:risk:behavior:manage', '管理用户行为管控', 'risk')
ON DUPLICATE KEY UPDATE
  `permission_name` = VALUES(`permission_name`),
  `permission_group` = VALUES(`permission_group`),
  `status` = 1;

INSERT INTO `iam_role_permission` (`role_id`, `permission_id`)
SELECT r.role_id, p.permission_id
FROM iam_role r
JOIN iam_permission p
WHERE r.role_code = 'SUPER_ADMIN'
ON DUPLICATE KEY UPDATE
  `status` = 1;

INSERT INTO `iam_role_permission` (`role_id`, `permission_id`)
SELECT r.role_id, p.permission_id
FROM iam_role r
JOIN iam_permission p
WHERE r.role_code = 'RISK_OPERATOR'
  AND p.permission_code IN (
    'risk:rule:manage',
    'risk:event:view',
    'risk:case:handle',
    'admin:risk:behavior:view',
    'admin:risk:behavior:manage'
  )
ON DUPLICATE KEY UPDATE
  `status` = 1;

INSERT INTO `risk_rule` (`rule_code`, `rule_name`, `event_type`, `rule_type`, `rule_config`, `decision_action`, `priority`)
VALUES
  ('RULE_LOGIN_BURST_IP', '登录频控-IP窗口限流', 'LOGIN', 'THRESHOLD', '{"windowMinutes":10,"maxCount":15,"subjectType":"IP"}', 'LIMIT', 10),
  ('RULE_GOODS_PUBLISH_BURST', '商品发布频控-用户小时限流', 'GOODS_PUBLISH', 'THRESHOLD', '{"windowMinutes":60,"maxCount":20,"subjectType":"USER"}', 'LIMIT', 20),
  ('RULE_ERRAND_PUBLISH_BURST', '跑腿发布频控-用户小时限流', 'ERRAND_PUBLISH', 'THRESHOLD', '{"windowMinutes":60,"maxCount":30,"subjectType":"USER"}', 'LIMIT', 20),
  ('RULE_CHAT_SENSITIVE_KEYWORD', '私聊敏感词复核', 'CHAT_SEND', 'KEYWORD', '{"field":"content","keywords":["加微信","vx","刷单","博彩","毒品","代考"]}', 'REVIEW', 30),
  ('RULE_AI_CHAT_SENSITIVE_KEYWORD', '小Q敏感词复核', 'AI_CHAT_SEND', 'KEYWORD', '{"field":"content","keywords":["代写论文","代考","博彩","毒品","办证"]}', 'REVIEW', 30)
ON DUPLICATE KEY UPDATE
  `rule_name` = VALUES(`rule_name`),
  `event_type` = VALUES(`event_type`),
  `rule_type` = VALUES(`rule_type`),
  `rule_config` = VALUES(`rule_config`),
  `decision_action` = VALUES(`decision_action`),
  `priority` = VALUES(`priority`),
  `status` = 1;

SET FOREIGN_KEY_CHECKS = 1;
