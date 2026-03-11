-- =====================================================
-- Nowl Database Initialization Script
-- Target DB: nowl
-- Usage:
--   mysql -u<user> -p < sql/nowl_init.sql
-- =====================================================
-- =====================================================
-- Nowl (nowl) Full Schema
-- Version: v1.0.0 (Breaking, No Backward Compatibility)
-- =====================================================

CREATE DATABASE IF NOT EXISTS `nowl` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `nowl`;

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
  ('risk:mode:view', '查看风控模式', 'risk'),
  ('risk:mode:manage', '管理风控模式', 'risk'),
  ('risk:list:view', '查看黑白名单', 'risk'),
  ('risk:list:manage', '管理黑白名单', 'risk'),
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
    'risk:mode:view',
    'risk:mode:manage',
    'risk:list:view',
    'risk:list:manage',
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

-- =====================================================
-- Extra Seed: Item Category (Idempotent)
-- =====================================================
-- 10个一级分类 + 对应二级分类（可重复执行）
-- 表：item_category(category_id, category_name, parent_id, sort, status, create_time, update_time)

START TRANSACTION;

-- 1) 一级分类（parent_id = 0）
INSERT INTO item_category (category_name, parent_id, sort, status)
SELECT v.category_name, 0, v.sort, 1
FROM (
    SELECT '教辅教材' AS category_name, 1 AS sort
    UNION ALL SELECT '电子产品', 2
    UNION ALL SELECT '电子资料', 3
    UNION ALL SELECT '生活用品', 4
    UNION ALL SELECT '学习办公', 5
    UNION ALL SELECT '服饰鞋包', 6
    UNION ALL SELECT '运动健身', 7
    UNION ALL SELECT '美妆个护', 8
    UNION ALL SELECT '宿舍电器', 9
    UNION ALL SELECT '文娱周边', 10
) v
LEFT JOIN item_category c
    ON c.category_name = v.category_name
   AND c.parent_id = 0
WHERE c.category_id IS NULL;

-- 2) 二级分类（按父分类名关联 parent_id）
INSERT INTO item_category (category_name, parent_id, sort, status)
SELECT v.category_name, p.category_id, v.sort, 1
FROM (
    -- 教辅教材
    SELECT '教辅教材' AS parent_name, '专业课教材' AS category_name, 1 AS sort
    UNION ALL SELECT '教辅教材', '公共课教材', 2
    UNION ALL SELECT '教辅教材', '考研教材', 3
    UNION ALL SELECT '教辅教材', '考公教材', 4
    UNION ALL SELECT '教辅教材', '语言考试资料', 5
    UNION ALL SELECT '教辅教材', '笔记讲义', 6

    -- 电子产品
    UNION ALL SELECT '电子产品', '手机', 1
    UNION ALL SELECT '电子产品', '笔记本电脑', 2
    UNION ALL SELECT '电子产品', '平板电脑', 3
    UNION ALL SELECT '电子产品', '耳机音箱', 4
    UNION ALL SELECT '电子产品', '相机摄影', 5
    UNION ALL SELECT '电子产品', '游戏设备', 6

    -- 电子资料
    UNION ALL SELECT '电子资料', '课程PPT/讲义', 1
    UNION ALL SELECT '电子资料', '历年真题', 2
    UNION ALL SELECT '电子资料', '考研网课资料', 3
    UNION ALL SELECT '电子资料', '考公网课资料', 4
    UNION ALL SELECT '电子资料', '编程开发资料', 5
    UNION ALL SELECT '电子资料', '设计素材模板', 6

    -- 生活用品
    UNION ALL SELECT '生活用品', '收纳整理', 1
    UNION ALL SELECT '生活用品', '床上用品', 2
    UNION ALL SELECT '生活用品', '洗护用品', 3
    UNION ALL SELECT '生活用品', '厨房餐具', 4
    UNION ALL SELECT '生活用品', '清洁用品', 5
    UNION ALL SELECT '生活用品', '雨伞水杯', 6

    -- 学习办公
    UNION ALL SELECT '学习办公', '文具用品', 1
    UNION ALL SELECT '学习办公', '台灯护眼', 2
    UNION ALL SELECT '学习办公', '计算器', 3
    UNION ALL SELECT '学习办公', '打印设备', 4
    UNION ALL SELECT '学习办公', '桌椅支架', 5
    UNION ALL SELECT '学习办公', '文件夹活页', 6

    -- 服饰鞋包
    UNION ALL SELECT '服饰鞋包', '上衣外套', 1
    UNION ALL SELECT '服饰鞋包', '裤装裙装', 2
    UNION ALL SELECT '服饰鞋包', '运动鞋', 3
    UNION ALL SELECT '服饰鞋包', '包袋书包', 4
    UNION ALL SELECT '服饰鞋包', '配饰', 5
    UNION ALL SELECT '服饰鞋包', '正装礼服', 6

    -- 运动健身
    UNION ALL SELECT '运动健身', '球类用品', 1
    UNION ALL SELECT '运动健身', '羽网乒器材', 2
    UNION ALL SELECT '运动健身', '跑步装备', 3
    UNION ALL SELECT '运动健身', '力量训练', 4
    UNION ALL SELECT '运动健身', '瑜伽普拉提', 5
    UNION ALL SELECT '运动健身', '骑行滑板', 6

    -- 美妆个护
    UNION ALL SELECT '美妆个护', '护肤品', 1
    UNION ALL SELECT '美妆个护', '彩妆', 2
    UNION ALL SELECT '美妆个护', '香水香氛', 3
    UNION ALL SELECT '美妆个护', '个护电器', 4
    UNION ALL SELECT '美妆个护', '美发造型', 5
    UNION ALL SELECT '美妆个护', '护理工具', 6

    -- 宿舍电器
    UNION ALL SELECT '宿舍电器', '小风扇', 1
    UNION ALL SELECT '宿舍电器', '电煮锅', 2
    UNION ALL SELECT '宿舍电器', '加湿器', 3
    UNION ALL SELECT '宿舍电器', '电热毯', 4
    UNION ALL SELECT '宿舍电器', '吹风机', 5
    UNION ALL SELECT '宿舍电器', '路由器', 6

    -- 文娱周边
    UNION ALL SELECT '文娱周边', '书籍小说', 1
    UNION ALL SELECT '文娱周边', '乐器', 2
    UNION ALL SELECT '文娱周边', '手办潮玩', 3
    UNION ALL SELECT '文娱周边', '桌游卡牌', 4
    UNION ALL SELECT '文娱周边', '演出票券', 5
    UNION ALL SELECT '文娱周边', '社团活动物资', 6
) v
JOIN item_category p
  ON p.category_name = v.parent_name
 AND p.parent_id = 0
LEFT JOIN item_category c
  ON c.category_name = v.category_name
 AND c.parent_id = p.category_id
WHERE c.category_id IS NULL;

COMMIT;

-- =====================================================
-- Extra Seed: School/Campus Dataset
-- (Converted to INSERT IGNORE for re-runnable import)
-- =====================================================
-- 双一流口径学校/校区插入数据
-- 数据来源: static-data.gaokao.cn (dualclass/list.json + school/list.json + school/poi/{school_id}.json)
-- 生成时间: 2026-02-15
-- 说明: school_code/campus_code 按顺序生成；status=1

INSERT IGNORE INTO `school_info` (`school_code`, `school_name`, `campus_code`, `campus_name`, `status`) VALUES
('SC0001', '北京大学', 'CP0001', '燕园校区（校本部）', 1),
('SC0001', '北京大学', 'CP0002', '医学部校区', 1),
('SC0001', '北京大学', 'CP0003', '昌平校区', 1),
('SC0001', '北京大学', 'CP0004', '大兴校区', 1),
('SC0001', '北京大学', 'CP0005', '无锡校区', 1),
('SC0001', '北京大学', 'CP0006', '深圳研究生院', 1),
('SC0001', '北京大学', 'CP0007', '昌平新校区', 1),
('SC0002', '清华大学', 'CP0008', '校本部', 1),
('SC0003', '浙江大学', 'CP0009', '紫金港校区', 1),
('SC0003', '浙江大学', 'CP0010', '宁波校区', 1),
('SC0003', '浙江大学', 'CP0011', '玉泉校区', 1),
('SC0003', '浙江大学', 'CP0012', '西溪校区', 1),
('SC0003', '浙江大学', 'CP0013', '华家池校区', 1),
('SC0003', '浙江大学', 'CP0014', '之江校区', 1),
('SC0003', '浙江大学', 'CP0015', '舟山校区', 1),
('SC0003', '浙江大学', 'CP0016', '海宁校区', 1),
('SC0004', '复旦大学', 'CP0017', '邯郸校区', 1),
('SC0004', '复旦大学', 'CP0018', '枫林校区', 1),
('SC0004', '复旦大学', 'CP0019', '张江校区', 1),
('SC0004', '复旦大学', 'CP0020', '江湾校区', 1),
('SC0005', '上海交通大学', 'CP0021', '闵行校区', 1),
('SC0005', '上海交通大学', 'CP0022', '徐汇校区', 1),
('SC0005', '上海交通大学', 'CP0023', '黄浦校区', 1),
('SC0005', '上海交通大学', 'CP0024', '长宁校区', 1),
('SC0005', '上海交通大学', 'CP0025', '浦东校区', 1),
('SC0006', '南京大学', 'CP0026', '仙林校区', 1),
('SC0006', '南京大学', 'CP0027', '鼓楼校区', 1),
('SC0006', '南京大学', 'CP0028', '浦口校区', 1),
('SC0006', '南京大学', 'CP0029', '苏州校区', 1),
('SC0007', '中国人民大学', 'CP0030', '校本部', 1),
('SC0007', '中国人民大学', 'CP0031', '苏州校区', 1),
('SC0007', '中国人民大学', 'CP0032', '深圳研究院校区', 1),
('SC0008', '北京师范大学', 'CP0033', '北京校区', 1),
('SC0008', '北京师范大学', 'CP0034', '珠海校区', 1),
('SC0009', '东南大学', 'CP0035', '四牌楼校区', 1),
('SC0009', '东南大学', 'CP0036', '九龙湖校区', 1),
('SC0009', '东南大学', 'CP0037', '丁家桥校区', 1),
('SC0009', '东南大学', 'CP0038', '无锡校区', 1),
('SC0009', '东南大学', 'CP0039', '苏州校区', 1),
('SC0010', '武汉大学', 'CP0040', '珞珈山主校区', 1),
('SC0010', '武汉大学', 'CP0041', '医学部校区', 1),
('SC0011', '中国科学技术大学', 'CP0042', '东校区', 1),
('SC0011', '中国科学技术大学', 'CP0043', '西校区', 1),
('SC0011', '中国科学技术大学', 'CP0044', '南校区', 1),
('SC0011', '中国科学技术大学', 'CP0045', '中校区', 1),
('SC0011', '中国科学技术大学', 'CP0046', '北校区', 1),
('SC0012', '中山大学', 'CP0047', '广州校区南校园', 1),
('SC0012', '中山大学', 'CP0048', '广州校区北校园', 1),
('SC0012', '中山大学', 'CP0049', '广州校区东校园', 1),
('SC0012', '中山大学', 'CP0050', '珠海校区', 1),
('SC0012', '中山大学', 'CP0051', '深圳校区', 1),
('SC0013', '华中科技大学', 'CP0052', '主校区', 1),
('SC0013', '华中科技大学', 'CP0053', '同济医学院校区', 1),
('SC0014', '中国农业大学', 'CP0054', '东校区', 1),
('SC0014', '中国农业大学', 'CP0055', '西校区', 1),
('SC0014', '中国农业大学', 'CP0056', '涿州教学实验场', 1),
('SC0014', '中国农业大学', 'CP0057', '烟台校区', 1),
('SC0015', '哈尔滨工业大学', 'CP0058', '哈尔滨校区', 1),
('SC0015', '哈尔滨工业大学', 'CP0059', '威海校区', 1),
('SC0015', '哈尔滨工业大学', 'CP0060', '深圳校区', 1),
('SC0016', '北京航空航天大学', 'CP0061', '学院路校区', 1),
('SC0016', '北京航空航天大学', 'CP0062', '沙河校区', 1),
('SC0017', '同济大学', 'CP0063', '四平路校区', 1),
('SC0017', '同济大学', 'CP0064', '嘉定校区', 1),
('SC0017', '同济大学', 'CP0065', '沪西校区', 1),
('SC0017', '同济大学', 'CP0066', '沪北校区', 1),
('SC0018', '西安交通大学', 'CP0067', '兴庆校区', 1),
('SC0018', '西安交通大学', 'CP0068', '雁塔校区', 1),
('SC0018', '西安交通大学', 'CP0069', '曲江校区', 1),
('SC0018', '西安交通大学', 'CP0070', '中国西部科技创新港校区', 1),
('SC0019', '南开大学', 'CP0071', '八里台校区', 1),
('SC0019', '南开大学', 'CP0072', '津南校区', 1),
('SC0019', '南开大学', 'CP0073', '泰达校区', 1),
('SC0020', '四川大学', 'CP0074', '望江校区', 1),
('SC0020', '四川大学', 'CP0075', '华西校区', 1),
('SC0020', '四川大学', 'CP0076', '江安校区', 1),
('SC0021', '厦门大学', 'CP0077', '思明校区', 1),
('SC0021', '厦门大学', 'CP0078', '海韵园区', 1),
('SC0021', '厦门大学', 'CP0079', '漳州校区', 1),
('SC0021', '厦门大学', 'CP0080', '翔安校区', 1),
('SC0021', '厦门大学', 'CP0081', '马来西亚分校', 1),
('SC0022', '吉林大学', 'CP0082', '前卫北区', 1),
('SC0022', '吉林大学', 'CP0083', '前卫南区', 1),
('SC0022', '吉林大学', 'CP0084', '南岭校区', 1),
('SC0022', '吉林大学', 'CP0085', '新民校区', 1),
('SC0022', '吉林大学', 'CP0086', '朝阳校区', 1),
('SC0022', '吉林大学', 'CP0087', '和平校区', 1),
('SC0022', '吉林大学', 'CP0088', '南湖校区', 1),
('SC0023', '东北师范大学', 'CP0089', '自由校区', 1),
('SC0023', '东北师范大学', 'CP0090', '净月校区', 1),
('SC0024', '天津大学', 'CP0091', '卫津路校区', 1),
('SC0024', '天津大学', 'CP0092', '北洋园校区', 1),
('SC0024', '天津大学', 'CP0093', '滨海工业研究院校区', 1),
('SC0025', '中南大学', 'CP0094', '校本部', 1),
('SC0025', '中南大学', 'CP0095', '新校区', 1),
('SC0025', '中南大学', 'CP0096', '南校区', 1),
('SC0025', '中南大学', 'CP0097', '湘雅新校区', 1),
('SC0025', '中南大学', 'CP0098', '铁道校区', 1),
('SC0026', '华中农业大学', 'CP0099', '校本部', 1),
('SC0027', '北京协和医学院', 'CP0100', '校本部', 1),
('SC0028', '中国人民解放军国防科技大学', 'CP0101', '长沙校区', 1),
('SC0028', '中国人民解放军国防科技大学', 'CP0102', '合肥校区', 1),
('SC0028', '中国人民解放军国防科技大学', 'CP0103', '武汉校区', 1),
('SC0028', '中国人民解放军国防科技大学', 'CP0104', '南京校区', 1),
('SC0029', '兰州大学', 'CP0105', '榆中校区', 1),
('SC0029', '兰州大学', 'CP0106', '城关校区', 1),
('SC0030', '华南理工大学', 'CP0107', '五山校区', 1),
('SC0030', '华南理工大学', 'CP0108', '大学城校区', 1),
('SC0030', '华南理工大学', 'CP0109', '广州国际校区', 1),
('SC0031', '山东大学', 'CP0110', '中心校区', 1),
('SC0031', '山东大学', 'CP0111', '千佛山校区', 1),
('SC0031', '山东大学', 'CP0112', '趵突泉校区', 1),
('SC0031', '山东大学', 'CP0113', '洪家楼校区', 1),
('SC0031', '山东大学', 'CP0114', '软件园校区', 1),
('SC0031', '山东大学', 'CP0115', '兴隆山校区', 1),
('SC0031', '山东大学', 'CP0116', '威海校区', 1),
('SC0031', '山东大学', 'CP0117', '青岛校区', 1),
('SC0032', '北京理工大学', 'CP0118', '中关村校区', 1),
('SC0032', '北京理工大学', 'CP0119', '良乡校区', 1),
('SC0032', '北京理工大学', 'CP0120', '西山校区', 1),
('SC0032', '北京理工大学', 'CP0121', '怀来校区', 1),
('SC0032', '北京理工大学', 'CP0122', '珠海校区', 1),
('SC0033', '北京科技大学', 'CP0123', '海淀校区', 1),
('SC0033', '北京科技大学', 'CP0124', '管庄校区', 1),
('SC0034', '湖南大学', 'CP0125', '南校区', 1),
('SC0034', '湖南大学', 'CP0126', '财院校区', 1),
('SC0035', '北京中医药大学', 'CP0127', '和平街校区', 1),
('SC0035', '北京中医药大学', 'CP0128', '良乡校区', 1),
('SC0035', '北京中医药大学', 'CP0129', '望京校区', 1),
('SC0036', '郑州大学', 'CP0130', '校本部', 1),
('SC0036', '郑州大学', 'CP0131', '南校区', 1),
('SC0036', '郑州大学', 'CP0132', '北校区', 1),
('SC0036', '郑州大学', 'CP0133', '东校区', 1),
('SC0036', '郑州大学', 'CP0134', '洛阳产业技术研究院', 1),
('SC0037', '新疆大学', 'CP0135', '红湖校区', 1),
('SC0037', '新疆大学', 'CP0136', '博达校区', 1),
('SC0037', '新疆大学', 'CP0137', '友好校区', 1),
('SC0038', '南京航空航天大学', 'CP0138', '明故宫校区', 1),
('SC0038', '南京航空航天大学', 'CP0139', '将军路校区', 1),
('SC0038', '南京航空航天大学', 'CP0140', '天目湖校区', 1),
('SC0039', '西北工业大学', 'CP0141', '友谊校区', 1),
('SC0039', '西北工业大学', 'CP0142', '长安校区', 1),
('SC0040', '重庆大学', 'CP0143', 'A校区', 1),
('SC0040', '重庆大学', 'CP0144', 'B校区', 1),
('SC0040', '重庆大学', 'CP0145', 'C校区', 1),
('SC0040', '重庆大学', 'CP0146', '虎溪校区', 1),
('SC0040', '重庆大学', 'CP0147', '两江校区', 1),
('SC0041', '华东师范大学', 'CP0148', '闵行校区', 1),
('SC0041', '华东师范大学', 'CP0149', '普陀校区', 1),
('SC0042', '华东理工大学', 'CP0150', '徐汇校区', 1),
('SC0042', '华东理工大学', 'CP0151', '奉贤校区', 1),
('SC0042', '华东理工大学', 'CP0152', '金山校区', 1),
('SC0043', '大连理工大学', 'CP0153', '大连凌水主校区', 1),
('SC0043', '大连理工大学', 'CP0154', '开发区校区', 1),
('SC0044', '华中师范大学', 'CP0155', '校本部', 1),
('SC0044', '华中师范大学', 'CP0156', '南湖校区', 1),
('SC0045', '云南大学', 'CP0157', '东陆校区', 1),
('SC0045', '云南大学', 'CP0158', '呈贡校区', 1),
('SC0046', '西北大学', 'CP0159', '长安校区', 1),
('SC0046', '西北大学', 'CP0160', '太白校区', 1),
('SC0046', '西北大学', 'CP0161', '桃园校区', 1),
('SC0047', '北京林业大学', 'CP0162', '校本部', 1),
('SC0048', '北京邮电大学', 'CP0163', '西土城路校区', 1),
('SC0048', '北京邮电大学', 'CP0164', '沙河校区', 1),
('SC0048', '北京邮电大学', 'CP0165', '海南校区', 1),
('SC0049', '西安电子科技大学', 'CP0166', '南校区', 1),
('SC0049', '西安电子科技大学', 'CP0167', '北校区', 1),
('SC0050', '中国海洋大学', 'CP0168', '崂山校区', 1),
('SC0050', '中国海洋大学', 'CP0169', '鱼山校区', 1),
('SC0050', '中国海洋大学', 'CP0170', '浮山校区', 1),
('SC0050', '中国海洋大学', 'CP0171', '西海岸校区', 1),
('SC0051', '江南大学', 'CP0172', '蠡湖校区', 1),
('SC0051', '江南大学', 'CP0173', '霞客湾校区', 1),
('SC0052', '中国矿业大学', 'CP0174', '南湖校区', 1),
('SC0052', '中国矿业大学', 'CP0175', '文昌校区', 1),
('SC0053', '南京农业大学', 'CP0176', '卫岗校区', 1),
('SC0053', '南京农业大学', 'CP0177', '白马教学科研基地', 1),
('SC0053', '南京农业大学', 'CP0178', '浦口校区', 1),
('SC0053', '南京农业大学', 'CP0179', '滨江校区', 1),
('SC0054', '河海大学', 'CP0180', '西康路校区', 1),
('SC0054', '河海大学', 'CP0181', '江宁校区', 1),
('SC0054', '河海大学', 'CP0182', '常州校区', 1),
('SC0055', '中国石油大学（华东）', 'CP0183', '唐岛湾校区', 1),
('SC0055', '中国石油大学（华东）', 'CP0184', '东营科教园区', 1),
('SC0055', '中国石油大学（华东）', 'CP0185', '古镇口校区', 1),
('SC0056', '中国地质大学（武汉）', 'CP0186', '南望山校区', 1),
('SC0056', '中国地质大学（武汉）', 'CP0187', '未来城校区', 1),
('SC0057', '东北大学', 'CP0188', '南湖校区', 1),
('SC0057', '东北大学', 'CP0189', '浑南校区', 1),
('SC0057', '东北大学', 'CP0190', '沈河校区', 1),
('SC0057', '东北大学', 'CP0191', '秦皇岛分校', 1),
('SC0058', '东华大学', 'CP0192', '延安路校区', 1),
('SC0058', '东华大学', 'CP0193', '松江校区', 1),
('SC0058', '东华大学', 'CP0194', '新华路校区', 1),
('SC0059', '山西大学', 'CP0195', '坞城校区', 1),
('SC0059', '山西大学', 'CP0196', '大东关校区', 1),
('SC0059', '山西大学', 'CP0197', '东山校区', 1),
('SC0060', '上海中医药大学', 'CP0198', '校本部', 1),
('SC0061', '西北农林科技大学', 'CP0199', '北校区', 1),
('SC0061', '西北农林科技大学', 'CP0200', '南校区', 1),
('SC0062', '东北林业大学', 'CP0201', '校本部', 1),
('SC0063', '中国传媒大学', 'CP0202', '校本部', 1),
('SC0064', '中央美术学院', 'CP0203', '望京校区', 1),
('SC0064', '中央美术学院', 'CP0204', '燕郊校区', 1),
('SC0064', '中央美术学院', 'CP0205', '后沙峪校区', 1),
('SC0064', '中央美术学院', 'CP0206', '上海校区', 1),
('SC0065', '电子科技大学', 'CP0207', '清水河校区', 1),
('SC0065', '电子科技大学', 'CP0208', '沙河校区', 1),
('SC0065', '电子科技大学', 'CP0209', '九里堤校区', 1),
('SC0066', '中国地质大学（北京）', 'CP0210', '校本部', 1),
('SC0067', '中国石油大学（北京）', 'CP0211', '校本部', 1),
('SC0068', '西南大学', 'CP0212', '北碚校区', 1),
('SC0068', '西南大学', 'CP0213', '荣昌校区', 1),
('SC0069', '中国矿业大学（北京）', 'CP0214', '学院路校区', 1),
('SC0069', '中国矿业大学（北京）', 'CP0215', '沙河校区', 1),
('SC0070', '中国科学院大学', 'CP0216', '玉泉路校区', 1),
('SC0070', '中国科学院大学', 'CP0217', '雁栖湖校区', 1),
('SC0070', '中国科学院大学', 'CP0218', '奥运村校区', 1),
('SC0070', '中国科学院大学', 'CP0219', '中关村校区', 1),
('SC0071', '北京工业大学', 'CP0220', '校本部', 1),
('SC0071', '北京工业大学', 'CP0221', '中蓝校区', 1),
('SC0071', '北京工业大学', 'CP0222', '管庄校区', 1),
('SC0071', '北京工业大学', 'CP0223', '花园村校区', 1),
('SC0071', '北京工业大学', 'CP0224', '琉璃井校区', 1),
('SC0071', '北京工业大学', 'CP0225', '惠新东街校区', 1),
('SC0071', '北京工业大学', 'CP0226', '通州校区', 1),
('SC0072', '内蒙古大学', 'CP0227', '赛罕校区', 1),
('SC0072', '内蒙古大学', 'CP0228', '玉泉校区', 1),
('SC0072', '内蒙古大学', 'CP0229', '满洲里学院', 1),
('SC0073', '大连海事大学', 'CP0230', '东山校区', 1),
('SC0073', '大连海事大学', 'CP0231', '西山校区', 1),
('SC0074', '长安大学', 'CP0232', '北校区', 1),
('SC0074', '长安大学', 'CP0233', '南校区', 1),
('SC0075', '北京交通大学', 'CP0234', '西校区', 1),
('SC0075', '北京交通大学', 'CP0235', '东校区', 1),
('SC0075', '北京交通大学', 'CP0236', '威海校区', 1),
('SC0076', '北京外国语大学', 'CP0237', '东校区', 1),
('SC0076', '北京外国语大学', 'CP0238', '西校区', 1),
('SC0077', '中央音乐学院', 'CP0239', '校本部', 1),
('SC0078', '河北工业大学', 'CP0240', '北辰校区', 1),
('SC0078', '河北工业大学', 'CP0241', '红桥北院', 1),
('SC0078', '河北工业大学', 'CP0242', '芬兰校区', 1),
('SC0078', '河北工业大学', 'CP0243', '红桥南院', 1),
('SC0078', '河北工业大学', 'CP0244', '红桥东院', 1),
('SC0079', '中央民族大学', 'CP0245', '海淀校区', 1),
('SC0079', '中央民族大学', 'CP0246', '海南国际学院', 1),
('SC0079', '中央民族大学', 'CP0247', '丰台校区', 1),
('SC0080', '辽宁大学', 'CP0248', '崇山校区', 1),
('SC0080', '辽宁大学', 'CP0249', '蒲河校区', 1),
('SC0080', '辽宁大学', 'CP0250', '辽阳武圣校区', 1),
('SC0081', '西南交通大学', 'CP0251', '犀浦校区', 1),
('SC0081', '西南交通大学', 'CP0252', '九里校区', 1),
('SC0081', '西南交通大学', 'CP0253', '峨眉校区', 1),
('SC0081', '西南交通大学', 'CP0254', '成都东部（国际）校区', 1),
('SC0082', '对外经济贸易大学', 'CP0255', '校本部', 1),
('SC0083', '湖南师范大学', 'CP0256', '二里半校区', 1),
('SC0083', '湖南师范大学', 'CP0257', '南院校区', 1),
('SC0083', '湖南师范大学', 'CP0258', '咸嘉湖校区', 1),
('SC0083', '湖南师范大学', 'CP0259', '桃花坪校区', 1),
('SC0083', '湖南师范大学', 'CP0260', '张公岭校区', 1),
('SC0084', '合肥工业大学', 'CP0261', '屯溪路校区', 1),
('SC0084', '合肥工业大学', 'CP0262', '翡翠湖校区', 1),
('SC0084', '合肥工业大学', 'CP0263', '六安路校区', 1),
('SC0084', '合肥工业大学', 'CP0264', '合肥工业大学智能制造技术研究院', 1),
('SC0085', '安徽大学', 'CP0265', '磬苑校区', 1),
('SC0085', '安徽大学', 'CP0266', '龙河校区', 1),
('SC0086', '上海大学', 'CP0267', '宝山校区', 1),
('SC0086', '上海大学', 'CP0268', '延长校区', 1),
('SC0086', '上海大学', 'CP0269', '嘉定校区', 1),
('SC0087', '天津医科大学', 'CP0270', '气象台路校区', 1),
('SC0087', '天津医科大学', 'CP0271', '广东路校区', 1),
('SC0088', '天津工业大学', 'CP0272', '校本部', 1),
('SC0089', '天津中医药大学', 'CP0273', '校本部', 1),
('SC0090', '广西大学', 'CP0274', '校本部', 1),
('SC0091', '华南师范大学', 'CP0275', '广州校区石牌校园', 1),
('SC0091', '华南师范大学', 'CP0276', '广州校区大学城校园', 1),
('SC0091', '华南师范大学', 'CP0277', '佛山校区南海校园', 1),
('SC0091', '华南师范大学', 'CP0278', '汕尾校区滨海校园', 1),
('SC0092', '四川农业大学', 'CP0279', '雅安校区', 1),
('SC0092', '四川农业大学', 'CP0280', '成都校区', 1),
('SC0092', '四川农业大学', 'CP0281', '都江堰校区', 1),
('SC0093', '西南财经大学', 'CP0282', '柳林校区', 1),
('SC0093', '西南财经大学', 'CP0283', '光华校区', 1),
('SC0094', '福州大学', 'CP0284', '旗山校区', 1),
('SC0094', '福州大学', 'CP0285', '铜盘校区', 1),
('SC0094', '福州大学', 'CP0286', '集美校区', 1),
('SC0094', '福州大学', 'CP0287', '晋江校区', 1),
('SC0094', '福州大学', 'CP0288', '怡山校区', 1),
('SC0094', '福州大学', 'CP0289', '泉港校区', 1),
('SC0094', '福州大学', 'CP0290', '鼓浪屿校区', 1),
('SC0095', '暨南大学', 'CP0291', '石牌校区', 1),
('SC0095', '暨南大学', 'CP0292', '华文校区', 1),
('SC0095', '暨南大学', 'CP0293', '深圳校区', 1),
('SC0095', '暨南大学', 'CP0294', '珠海校区', 1),
('SC0095', '暨南大学', 'CP0295', '番禺校区', 1),
('SC0096', '南昌大学', 'CP0296', '前湖校区', 1),
('SC0096', '南昌大学', 'CP0297', '青山湖校区', 1),
('SC0096', '南昌大学', 'CP0298', '东湖校区', 1),
('SC0097', '南京理工大学', 'CP0299', '南京校区', 1),
('SC0097', '南京理工大学', 'CP0300', '江阴校区', 1),
('SC0097', '南京理工大学', 'CP0301', '盱眙校区', 1),
('SC0098', '南京师范大学', 'CP0302', '随园校区', 1),
('SC0098', '南京师范大学', 'CP0303', '仙林校区', 1),
('SC0098', '南京师范大学', 'CP0304', '紫金校区', 1),
('SC0099', '中国药科大学', 'CP0305', '玄武门校区', 1),
('SC0099', '中国药科大学', 'CP0306', '江宁校区', 1),
('SC0100', '苏州大学', 'CP0307', '天赐庄校区', 1),
('SC0100', '苏州大学', 'CP0308', '独墅湖校区', 1),
('SC0100', '苏州大学', 'CP0309', '阳澄湖校区', 1),
('SC0100', '苏州大学', 'CP0310', '未来校区', 1),
('SC0101', '哈尔滨工程大学', 'CP0311', '校本部', 1),
('SC0102', '武汉理工大学', 'CP0312', '马房山校区', 1),
('SC0102', '武汉理工大学', 'CP0313', '余家头校区', 1),
('SC0102', '武汉理工大学', 'CP0314', '南湖校区', 1),
('SC0103', '延边大学', 'CP0315', '延吉校区', 1),
('SC0103', '延边大学', 'CP0316', '科学技术学院校区', 1),
('SC0103', '延边大学', 'CP0317', '师范分院校区', 1),
('SC0103', '延边大学', 'CP0318', '珲春校区', 1),
('SC0104', '上海财经大学', 'CP0319', '主校区', 1),
('SC0104', '上海财经大学', 'CP0320', '中山北一路校区', 1),
('SC0104', '上海财经大学', 'CP0321', '昆山路校区', 1),
('SC0104', '上海财经大学', 'CP0322', '武东路校区', 1),
('SC0104', '上海财经大学', 'CP0323', '武川路校区', 1),
('SC0105', '上海外国语大学', 'CP0324', '虹口校区', 1),
('SC0105', '上海外国语大学', 'CP0325', '松江校区', 1),
('SC0106', '东北农业大学', 'CP0326', '校本部', 1),
('SC0107', '太原理工大学', 'CP0327', '迎西校区', 1),
('SC0107', '太原理工大学', 'CP0328', '虎峪校区', 1),
('SC0107', '太原理工大学', 'CP0329', '柏林校区', 1),
('SC0107', '太原理工大学', 'CP0330', '明向校区', 1),
('SC0108', '南京邮电大学', 'CP0331', '仙林校区', 1),
('SC0108', '南京邮电大学', 'CP0332', '三牌楼校区', 1),
('SC0108', '南京邮电大学', 'CP0333', '锁金村校区', 1),
('SC0109', '南京中医药大学', 'CP0334', '仙林校区', 1),
('SC0109', '南京中医药大学', 'CP0335', '汉中门校区', 1),
('SC0109', '南京中医药大学', 'CP0336', '泰州校区', 1),
('SC0110', '南京信息工程大学', 'CP0337', '校本部', 1),
('SC0110', '南京信息工程大学', 'CP0338', '金牛湖校区', 1),
('SC0110', '南京信息工程大学', 'CP0339', '无锡校区（已转设为无锡学院）', 1),
('SC0111', '南京医科大学', 'CP0340', '江宁校区', 1),
('SC0111', '南京医科大学', 'CP0341', '五台校区', 1),
('SC0112', '南京林业大学', 'CP0342', '南京新庄校区', 1),
('SC0112', '南京林业大学', 'CP0343', '淮安校区', 1),
('SC0112', '南京林业大学', 'CP0344', '南京白马校区', 1),
('SC0113', '石河子大学', 'CP0345', '中校区', 1),
('SC0113', '石河子大学', 'CP0346', '北校区', 1),
('SC0113', '石河子大学', 'CP0347', '南校区', 1),
('SC0113', '石河子大学', 'CP0348', '东校区', 1),
('SC0113', '石河子大学', 'CP0349', '北苑新区', 1),
('SC0114', '成都理工大学', 'CP0350', '成都校区', 1),
('SC0114', '成都理工大学', 'CP0351', '宜宾校区', 1),
('SC0115', '宁波大学', 'CP0352', '主校区', 1),
('SC0115', '宁波大学', 'CP0353', '梅山校区', 1),
('SC0115', '宁波大学', 'CP0354', '植物园校区', 1),
('SC0116', '中国美术学院', 'CP0355', '南山校区', 1),
('SC0116', '中国美术学院', 'CP0356', '象山校区', 1),
('SC0116', '中国美术学院', 'CP0357', '张江校区', 1),
('SC0116', '中国美术学院', 'CP0358', '湘湖校区', 1),
('SC0116', '中国美术学院', 'CP0359', '良渚校区', 1),
('SC0117', '成都中医药大学', 'CP0360', '温江校区', 1),
('SC0117', '成都中医药大学', 'CP0361', '十二桥校区', 1),
('SC0117', '成都中医药大学', 'CP0362', '人南校区', 1),
('SC0117', '成都中医药大学', 'CP0363', '汪家拐校区', 1),
('SC0118', '西南石油大学', 'CP0364', '成都校区', 1),
('SC0118', '西南石油大学', 'CP0365', '南充校区', 1),
('SC0119', '华南农业大学', 'CP0366', '校本部', 1),
('SC0120', '广州中医药大学', 'CP0367', '大学城校区', 1),
('SC0120', '广州中医药大学', 'CP0368', '三元里校区', 1),
('SC0121', '广州医科大学', 'CP0369', '番禺校区', 1),
('SC0121', '广州医科大学', 'CP0370', '越秀校区', 1),
('SC0122', '上海海洋大学', 'CP0371', '临港新城校区', 1),
('SC0122', '上海海洋大学', 'CP0372', '军工路校区', 1),
('SC0123', '上海体育大学', 'CP0373', '杨浦校区', 1),
('SC0123', '上海体育大学', 'CP0374', '松江校区', 1),
('SC0123', '上海体育大学', 'CP0375', '徐汇校区', 1),
('SC0124', '上海音乐学院', 'CP0376', '汾阳路校区', 1),
('SC0124', '上海音乐学院', 'CP0377', '零陵路校区', 1),
('SC0125', '陕西师范大学', 'CP0378', '雁塔校区', 1),
('SC0125', '陕西师范大学', 'CP0379', '长安校区', 1),
('SC0126', '西藏大学', 'CP0380', '纳金校区', 1),
('SC0126', '西藏大学', 'CP0381', '河坝林校区', 1),
('SC0126', '西藏大学', 'CP0382', '罗布林卡医学院校区', 1),
('SC0126', '西藏大学', 'CP0383', '罗布林卡财经学院校区', 1),
('SC0127', '青海大学', 'CP0384', '校本部', 1),
('SC0127', '青海大学', 'CP0385', '藏医学院校区', 1),
('SC0127', '青海大学', 'CP0386', '昆仑学院校区', 1),
('SC0128', '湘潭大学', 'CP0387', '校本部', 1),
('SC0129', '中南财经政法大学', 'CP0388', '南湖校区', 1),
('SC0129', '中南财经政法大学', 'CP0389', '首义校区', 1),
('SC0130', '河南大学', 'CP0390', '龙子湖校区', 1),
('SC0130', '河南大学', 'CP0391', '明伦校区', 1),
('SC0130', '河南大学', 'CP0392', '金明校区', 1),
('SC0131', '海南大学', 'CP0393', '海甸校区', 1),
('SC0131', '海南大学', 'CP0394', '儋州校区', 1),
('SC0131', '海南大学', 'CP0395', '城西校区', 1),
('SC0131', '海南大学', 'CP0396', '观澜湖校区', 1),
('SC0131', '海南大学', 'CP0397', '崖州湾科教园校区', 1),
('SC0132', '宁夏大学', 'CP0398', '贺兰山校区', 1),
('SC0132', '宁夏大学', 'CP0399', '怀远校区', 1),
('SC0132', '宁夏大学', 'CP0400', '文萃校区', 1),
('SC0132', '宁夏大学', 'CP0401', '朔方校区', 1),
('SC0132', '宁夏大学', 'CP0402', '金凤校区', 1),
('SC0132', '宁夏大学', 'CP0403', '中卫校区', 1),
('SC0132', '宁夏大学', 'CP0404', '教学实验农场', 1),
('SC0133', '北京化工大学', 'CP0405', '朝阳校区', 1),
('SC0133', '北京化工大学', 'CP0406', '昌平校区', 1),
('SC0133', '北京化工大学', 'CP0407', '海淀校区', 1),
('SC0134', '中央戏剧学院', 'CP0408', '东城校区', 1),
('SC0134', '中央戏剧学院', 'CP0409', '昌平校区', 1),
('SC0135', '中央财经大学', 'CP0410', '学院南路校区', 1),
('SC0135', '中央财经大学', 'CP0411', '沙河校区', 1),
('SC0136', '中国政法大学', 'CP0412', '海淀校区', 1),
('SC0136', '中国政法大学', 'CP0413', '昌平校区', 1),
('SC0137', '中国音乐学院', 'CP0414', '校本部', 1),
('SC0138', '中国人民公安大学', 'CP0415', '木樨地校区', 1),
('SC0138', '中国人民公安大学', 'CP0416', '团河校区', 1),
('SC0139', '外交学院', 'CP0417', '展览馆路校区', 1),
('SC0139', '外交学院', 'CP0418', '沙河校区', 1),
('SC0140', '首都师范大学', 'CP0419', '校本部', 1),
('SC0140', '首都师范大学', 'CP0420', '良乡校区', 1),
('SC0140', '首都师范大学', 'CP0421', '北一区', 1),
('SC0140', '首都师范大学', 'CP0422', '东一区', 1),
('SC0140', '首都师范大学', 'CP0423', '东二区', 1),
('SC0140', '首都师范大学', 'CP0424', '北二区', 1),
('SC0141', '华北电力大学', 'CP0425', '保定一校区', 1),
('SC0141', '华北电力大学', 'CP0426', '保定二校区', 1),
('SC0141', '华北电力大学', 'CP0427', '北京校区', 1),
('SC0141', '华北电力大学', 'CP0428', '保定校区', 1),
('SC0142', '北京体育大学', 'CP0429', '校本部', 1),
('SC0143', '贵州大学', 'CP0430', '花溪东校区', 1),
('SC0143', '贵州大学', 'CP0431', '花溪南校区', 1),
('SC0143', '贵州大学', 'CP0432', '花溪西校区', 1),
('SC0144', '海军军医大学', 'CP0433', '校本部', 1),
('SC0145', '中国人民解放军空军军医大学', 'CP0434', '西京医院校区', 1),
('SC0145', '中国人民解放军空军军医大学', 'CP0435', '校本部', 1),
('SC0145', '中国人民解放军空军军医大学', 'CP0436', '唐都临床校区', 1),
('SC0145', '中国人民解放军空军军医大学', 'CP0437', '白求恩军医校区', 1),
('SC0146', '南方科技大学', 'CP0438', '校本部', 1),
('SC0147', '上海科技大学', 'CP0439', '浦东校区', 1),
('SC0147', '上海科技大学', 'CP0440', '岳阳路校区', 1);



