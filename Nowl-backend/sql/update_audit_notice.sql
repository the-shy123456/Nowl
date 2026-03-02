-- Add audit_reason column to goods_info table
ALTER TABLE goods_info ADD COLUMN audit_reason VARCHAR(500) COMMENT '审核不通过原因';

-- Add related_id column to sys_notice table
ALTER TABLE sys_notice ADD COLUMN related_id BIGINT COMMENT '关联ID（如商品ID）';
