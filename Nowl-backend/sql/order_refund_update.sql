-- 订单退款能力扩展（兼容 MySQL 5.7+/8.0，避免 IF NOT EXISTS 语法差异）
SET @db = DATABASE();

-- refund_status
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'order_info'
      AND COLUMN_NAME = 'refund_status'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE order_info ADD COLUMN refund_status TINYINT NOT NULL DEFAULT 0 COMMENT ''退款状态：0-无退款，1-待处理，2-已退款，3-已拒绝''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- refund_reason
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'order_info'
      AND COLUMN_NAME = 'refund_reason'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE order_info ADD COLUMN refund_reason VARCHAR(255) COMMENT ''退款申请原因''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- refund_amount
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'order_info'
      AND COLUMN_NAME = 'refund_amount'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE order_info ADD COLUMN refund_amount DECIMAL(10,2) COMMENT ''退款申请金额''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- refund_apply_time
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'order_info'
      AND COLUMN_NAME = 'refund_apply_time'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE order_info ADD COLUMN refund_apply_time DATETIME COMMENT ''退款申请时间''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- refund_deadline
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'order_info'
      AND COLUMN_NAME = 'refund_deadline'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE order_info ADD COLUMN refund_deadline DATETIME COMMENT ''退款处理截止时间''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- refund_process_time
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'order_info'
      AND COLUMN_NAME = 'refund_process_time'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE order_info ADD COLUMN refund_process_time DATETIME COMMENT ''退款处理时间''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- refund_processor_id
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'order_info'
      AND COLUMN_NAME = 'refund_processor_id'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE order_info ADD COLUMN refund_processor_id BIGINT COMMENT ''退款处理人ID''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- refund_process_remark
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'order_info'
      AND COLUMN_NAME = 'refund_process_remark'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE order_info ADD COLUMN refund_process_remark VARCHAR(255) COMMENT ''退款处理备注''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- refund_fast_track
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'order_info'
      AND COLUMN_NAME = 'refund_fast_track'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE order_info ADD COLUMN refund_fast_track TINYINT NOT NULL DEFAULT 0 COMMENT ''是否极速退款：0-否，1-是''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 索引 idx_order_refund_status_deadline
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'order_info'
      AND INDEX_NAME = 'idx_order_refund_status_deadline'
);
SET @sql = IF(
    @idx_exists = 0,
    'CREATE INDEX idx_order_refund_status_deadline ON order_info(refund_status, refund_deadline)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
