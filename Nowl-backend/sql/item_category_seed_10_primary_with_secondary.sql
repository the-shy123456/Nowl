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
