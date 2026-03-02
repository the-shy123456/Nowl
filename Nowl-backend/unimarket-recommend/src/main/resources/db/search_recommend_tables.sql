-- ============================================
-- UniMarket 搜索推荐系统数据库表
-- ============================================

-- 1. 用户行为日志表
CREATE TABLE IF NOT EXISTS `user_behavior_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `behavior_type` TINYINT NOT NULL COMMENT '行为类型: 1浏览 2收藏 3购买 4搜索',
    `product_id` BIGINT COMMENT '商品ID',
    `category_id` INT COMMENT '分类ID(冗余加速)',
    `keyword` VARCHAR(100) COMMENT '搜索关键词',
    `duration` INT COMMENT '浏览时长(秒)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_time` (`user_id`, `create_time`),
    INDEX `idx_product_behavior` (`product_id`, `behavior_type`),
    INDEX `idx_category` (`category_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为日志';

-- 2. 商品相似度表（物品协同过滤预计算结果）
CREATE TABLE IF NOT EXISTS `goods_similarity` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `similar_product_id` BIGINT NOT NULL COMMENT '相似商品ID',
    `similarity_score` DECIMAL(5,4) NOT NULL COMMENT '相似度分数(0-1)',
    `similarity_type` TINYINT DEFAULT 1 COMMENT '1协同过滤 2内容相似',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_product_similar_type` (`product_id`, `similar_product_id`, `similarity_type`),
    INDEX `idx_product_score` (`product_id`, `similarity_score` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品相似度矩阵';

-- 3. 用户偏好画像表
CREATE TABLE IF NOT EXISTS `user_preference` (
    `user_id` BIGINT PRIMARY KEY COMMENT '用户ID',
    `category_scores` JSON COMMENT '分类偏好 {"1":0.8,"3":0.5}',
    `price_preference` JSON COMMENT '价格偏好 {"min":50,"max":300,"avg":120}',
    `behavior_count` JSON COMMENT '行为统计 {"view":100,"collect":20,"buy":5}',
    `last_active_time` DATETIME COMMENT '最后活跃时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好画像';

-- 4. 搜索历史表
CREATE TABLE IF NOT EXISTS `search_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `keyword` VARCHAR(100) NOT NULL COMMENT '搜索词',
    `result_count` INT DEFAULT 0 COMMENT '结果数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_time` (`user_id`, `create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索历史';

-- 5. 热搜词统计表
CREATE TABLE IF NOT EXISTS `hot_search_word` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `keyword` VARCHAR(100) NOT NULL COMMENT '搜索词',
    `school_code` VARCHAR(20) DEFAULT 'ALL' COMMENT '学校编码',
    `search_count` INT DEFAULT 0 COMMENT '搜索次数',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    UNIQUE KEY `uk_keyword_school_date` (`keyword`, `school_code`, `stat_date`),
    INDEX `idx_date_count` (`stat_date`, `search_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热搜词统计';

-- 6. 商品表增加字段（如果不存在）
-- ALTER TABLE `goods_info` ADD COLUMN `view_count` INT DEFAULT 0 COMMENT '浏览量';
-- ALTER TABLE `goods_info` ADD COLUMN `hot_score` DECIMAL(10,2) DEFAULT 0 COMMENT '热度分';
-- ALTER TABLE `goods_info` ADD INDEX `idx_hot_score` (`hot_score` DESC);

-- 检查并添加 view_count 字段
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'goods_info'
    AND COLUMN_NAME = 'view_count'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE `goods_info` ADD COLUMN `view_count` INT DEFAULT 0 COMMENT ''浏览量''',
    'SELECT ''view_count column already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 hot_score 字段
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'goods_info'
    AND COLUMN_NAME = 'hot_score'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE `goods_info` ADD COLUMN `hot_score` DECIMAL(10,2) DEFAULT 0 COMMENT ''热度分''',
    'SELECT ''hot_score column already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
