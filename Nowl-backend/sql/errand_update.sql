-- 跑腿任务表字段更新
-- 执行时间: 2024年
-- 说明: 完善跑腿功能，新增地址、时间戳等字段

-- 新增取件地址
ALTER TABLE errand_task ADD COLUMN IF NOT EXISTS pickup_address VARCHAR(255) COMMENT '取件地址';

-- 新增送达地址
ALTER TABLE errand_task ADD COLUMN IF NOT EXISTS delivery_address VARCHAR(255) COMMENT '送达地址';

-- 新增备注信息
ALTER TABLE errand_task ADD COLUMN IF NOT EXISTS remark VARCHAR(500) COMMENT '备注信息';

-- 新增接单时间
ALTER TABLE errand_task ADD COLUMN IF NOT EXISTS accept_time DATETIME COMMENT '接单时间';

-- 新增送达时间（跑腿员上传凭证时间）
ALTER TABLE errand_task ADD COLUMN IF NOT EXISTS deliver_time DATETIME COMMENT '送达时间';

-- 新增确认时间（发布者确认完成时间）
ALTER TABLE errand_task ADD COLUMN IF NOT EXISTS confirm_time DATETIME COMMENT '确认时间';

-- 新增取消时间
ALTER TABLE errand_task ADD COLUMN IF NOT EXISTS cancel_time DATETIME COMMENT '取消时间';

-- 新增取消原因
ALTER TABLE errand_task ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(255) COMMENT '取消原因';

-- 新增审核状态（历史数据默认视为通过，避免存量任务不可见）
ALTER TABLE errand_task ADD COLUMN IF NOT EXISTS review_status TINYINT NOT NULL DEFAULT 1 COMMENT '审核状态：0待审核,1AI通过,2人工通过,3违规驳回,4待人工复核';

-- 新增审核原因
ALTER TABLE errand_task ADD COLUMN IF NOT EXISTS audit_reason VARCHAR(500) COMMENT '审核原因（驳回/复核说明）';

-- 创建索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_errand_publisher ON errand_task(publisher_id);
CREATE INDEX IF NOT EXISTS idx_errand_acceptor ON errand_task(acceptor_id);
CREATE INDEX IF NOT EXISTS idx_errand_status ON errand_task(task_status);
CREATE INDEX IF NOT EXISTS idx_errand_deliver_time ON errand_task(deliver_time);
CREATE INDEX IF NOT EXISTS idx_errand_review_status ON errand_task(review_status);

-- 状态说明更新:
-- 0: 待接单 (PENDING)
-- 1: 进行中 (IN_PROGRESS)
-- 2: 待确认 (PENDING_CONFIRM) - 新增状态
-- 3: 已完成 (COMPLETED)
-- 4: 已取消 (CANCELLED)
