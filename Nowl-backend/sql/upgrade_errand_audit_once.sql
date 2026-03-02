-- ============================================================
-- 跑腿审核一次性升级脚本（兼容不支持 ADD COLUMN IF NOT EXISTS 的 MySQL）
-- 用法：
--   1) USE unimarketnew;
--   2) source ./Nowl-backend/sql/upgrade_errand_audit_once.sql;
-- ============================================================

SET @db := DATABASE();

-- 1) errand_task.review_status
SET @sql := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db
        AND TABLE_NAME = 'errand_task'
        AND COLUMN_NAME = 'review_status'
    ),
    'SELECT ''skip: errand_task.review_status exists'' AS msg',
    'ALTER TABLE errand_task
       ADD COLUMN review_status TINYINT NOT NULL DEFAULT 1
       COMMENT ''审核状态：0待审核,1AI通过,2人工通过,3违规驳回,4待人工复核''
       AFTER task_status'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) errand_task.audit_reason
SET @sql := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db
        AND TABLE_NAME = 'errand_task'
        AND COLUMN_NAME = 'audit_reason'
    ),
    'SELECT ''skip: errand_task.audit_reason exists'' AS msg',
    'ALTER TABLE errand_task
       ADD COLUMN audit_reason VARCHAR(500)
       COMMENT ''审核原因（驳回/复核说明）''
       AFTER review_status'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 索引 idx_errand_review_status
SET @sql := (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = @db
        AND TABLE_NAME = 'errand_task'
        AND INDEX_NAME = 'idx_errand_review_status'
    ),
    'SELECT ''skip: idx_errand_review_status exists'' AS msg',
    'CREATE INDEX idx_errand_review_status ON errand_task(review_status)'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) 兜底修复历史数据（避免空值）
UPDATE errand_task
SET review_status = 1
WHERE review_status IS NULL;

-- 5) 新增权限点：admin:errand:audit
INSERT INTO iam_permission (permission_code, permission_name, permission_group, status, create_time, update_time)
VALUES ('admin:errand:audit', '复核跑腿任务', 'errand', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  permission_group = VALUES(permission_group),
  status = 1,
  update_time = NOW();

-- 6) 绑定到常用管理角色（不存在的角色会自动跳过）
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, status, create_time, update_time)
SELECT r.role_id, p.permission_id, 1, NOW(), NOW()
FROM iam_role r
JOIN iam_permission p ON p.permission_code = 'admin:errand:audit'
WHERE r.role_code IN ('SUPER_ADMIN', 'SCHOOL_ADMIN', 'CAMPUS_ADMIN', 'CONTENT_AUDITOR', 'CUSTOMER_SUPPORT');

-- 7) 校验输出
SELECT '=== COLUMN CHECK ===' AS section_name;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db
  AND TABLE_NAME = 'errand_task'
  AND COLUMN_NAME IN ('review_status', 'audit_reason')
ORDER BY COLUMN_NAME;

SELECT '=== INDEX CHECK ===' AS section_name;
SELECT INDEX_NAME, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = @db
  AND TABLE_NAME = 'errand_task'
  AND INDEX_NAME = 'idx_errand_review_status';

SELECT '=== PERMISSION CHECK ===' AS section_name;
SELECT permission_id, permission_code, permission_name, permission_group, status
FROM iam_permission
WHERE permission_code = 'admin:errand:audit';


