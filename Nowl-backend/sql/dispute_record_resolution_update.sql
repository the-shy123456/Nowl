-- 纠纷处理结果字段补充与历史回填
-- 为 dispute_record 增加“实际裁定退款金额 / 实际裁定扣除信用分”字段，
-- 并尝试从后台审计日志 audit_admin_operation 中回填历史数据。

SET @db = DATABASE();

-- resolved_refund_amount
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'dispute_record'
      AND COLUMN_NAME = 'resolved_refund_amount'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE dispute_record ADD COLUMN resolved_refund_amount DECIMAL(10,2) DEFAULT NULL COMMENT ''实际裁定退款金额''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- resolved_credit_penalty
SET @exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db
      AND TABLE_NAME = 'dispute_record'
      AND COLUMN_NAME = 'resolved_credit_penalty'
);
SET @sql = IF(
    @exists = 0,
    'ALTER TABLE dispute_record ADD COLUMN resolved_credit_penalty INT DEFAULT NULL COMMENT ''实际裁定扣除信用分''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 从后台审计日志回填历史处理参数
-- 审计日志中的 request_payload.params 结构示例：
-- refundAmount: ["12.50"]
-- deductCreditScore: ["5"]
UPDATE dispute_record d
JOIN (
    SELECT
        CAST(a.target_id AS UNSIGNED) AS dispute_id,
        CAST(
            NULLIF(
                JSON_UNQUOTE(JSON_EXTRACT(a.request_payload, '$.params.refundAmount[0]')),
                ''
            ) AS DECIMAL(10,2)
        ) AS resolved_refund_amount,
        CAST(
            NULLIF(
                JSON_UNQUOTE(JSON_EXTRACT(a.request_payload, '$.params.deductCreditScore[0]')),
                ''
            ) AS SIGNED
        ) AS resolved_credit_penalty
    FROM audit_admin_operation a
    JOIN (
        SELECT MAX(id) AS max_id
        FROM audit_admin_operation
        WHERE module = 'dispute'
          AND action = 'handle'
          AND target_type = 'DISPUTE'
          AND result_status = 'SUCCESS'
          AND target_id REGEXP '^[0-9]+$'
        GROUP BY CAST(target_id AS UNSIGNED)
    ) latest ON latest.max_id = a.id
) audit_fill ON audit_fill.dispute_id = d.record_id
SET d.resolved_refund_amount = COALESCE(d.resolved_refund_amount, audit_fill.resolved_refund_amount),
    d.resolved_credit_penalty = COALESCE(d.resolved_credit_penalty, audit_fill.resolved_credit_penalty)
WHERE d.resolved_refund_amount IS NULL
   OR d.resolved_credit_penalty IS NULL;
