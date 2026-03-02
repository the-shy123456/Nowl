-- 纠纷能力扩展：诉求项与双方最多3次补充回复（兼容 MySQL 5.7+/8.0）
SET @db = DATABASE();

-- claim_seller_credit_penalty
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'dispute_record'
      AND COLUMN_NAME = 'claim_seller_credit_penalty'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE dispute_record ADD COLUMN claim_seller_credit_penalty TINYINT NOT NULL DEFAULT 0 COMMENT ''申请扣除卖家信用分：0-否，1-是''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- claim_refund
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'dispute_record'
      AND COLUMN_NAME = 'claim_refund'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE dispute_record ADD COLUMN claim_refund TINYINT NOT NULL DEFAULT 0 COMMENT ''申请退还金额：0-否，1-是''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- claim_refund_amount
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'dispute_record'
      AND COLUMN_NAME = 'claim_refund_amount'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE dispute_record ADD COLUMN claim_refund_amount DECIMAL(10,2) DEFAULT NULL COMMENT ''申请退还金额''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- initiator_reply_count
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'dispute_record'
      AND COLUMN_NAME = 'initiator_reply_count'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE dispute_record ADD COLUMN initiator_reply_count INT NOT NULL DEFAULT 0 COMMENT ''发起人补充次数''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- related_reply_count
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'dispute_record'
      AND COLUMN_NAME = 'related_reply_count'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE dispute_record ADD COLUMN related_reply_count INT NOT NULL DEFAULT 0 COMMENT ''被投诉方补充次数''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- conversation_logs
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'dispute_record'
      AND COLUMN_NAME = 'conversation_logs'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE dispute_record ADD COLUMN conversation_logs TEXT COMMENT ''双方交流记录(JSON)''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
