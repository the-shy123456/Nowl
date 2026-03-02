-- 插入一级分类（parent_id=0，按校园交易热度排序）
INSERT INTO `item_category` (`category_name`, `parent_id`, `sort`, `status`) VALUES
                                                                                 ('教材教辅', 0, 1, 1),  -- 校园最高频，排第一
                                                                                 ('数码产品', 0, 2, 1),
                                                                                 ('生活用品', 0, 3, 1),
                                                                                 ('学习用品', 0, 4, 1),
                                                                                 ('运动器材', 0, 5, 1),
                                                                                 ('服饰鞋包', 0, 6, 1),
                                                                                 ('电子产品配件', 0, 7, 1),
                                                                                 ('文娱周边', 0, 8, 1),
                                                                                 ('考研考公资料', 0, 9, 1),
                                                                                 ('其他闲置', 0, 10, 1);

-- 插入教材教辅二级分类（parent_id=1，校园核心）
INSERT INTO `item_category` (`category_name`, `parent_id`, `sort`, `status`) VALUES
                                                                                 ('专业课教材', 1, 1, 1),
                                                                                 ('公共课教材', 1, 2, 1),  -- 高数/英语/思政等
                                                                                 ('考研教材', 1, 3, 1),
                                                                                 ('考公教材', 1, 4, 1),
                                                                                 ('考证资料', 1, 5, 1),  -- 四六级/教资/计算机等
                                                                                 ('教辅讲义', 1, 6, 1),
                                                                                 ('二手笔记', 1, 7, 1),  -- 学长学姐整理的笔记
                                                                                 ('期刊杂志', 1, 8, 1);

-- 插入数码产品二级分类（parent_id=2，校园高频）
INSERT INTO `item_category` (`category_name`, `parent_id`, `sort`, `status`) VALUES
                                                                                 ('手机', 2, 1, 1),
                                                                                 ('笔记本电脑', 2, 2, 1),
                                                                                 ('平板/Pad', 2, 3, 1),
                                                                                 ('耳机/音箱', 2, 4, 1),
                                                                                 ('相机/拍立得', 2, 5, 1),
                                                                                 ('充电宝/数据线', 2, 6, 1),
                                                                                 ('U盘/移动硬盘', 2, 7, 1),
                                                                                 ('游戏机/掌机', 2, 8, 1);

-- 插入生活用品二级分类（parent_id=3，宿舍/日常）
INSERT INTO `item_category` (`category_name`, `parent_id`, `sort`, `status`) VALUES
                                                                                 ('宿舍家具', 3, 1, 1),  -- 床帘/收纳柜/椅子等
                                                                                 ('洗护用品', 3, 2, 1),
                                                                                 ('厨房小电器', 3, 3, 1),  -- 煮蛋器/小煮锅/加湿器等
                                                                                 ('收纳整理', 3, 4, 1),
                                                                                 ('日用百货', 3, 5, 1),
                                                                                 ('军训用品', 3, 6, 1),  -- 专属校园场景
                                                                                 ('换季衣物收纳', 3, 7, 1);

-- 插入学习用品二级分类（parent_id=4）
INSERT INTO `item_category` (`category_name`, `parent_id`, `sort`, `status`) VALUES
                                                                                 ('文具', 4, 1, 1),  -- 笔/本/文件夹等
                                                                                 ('台灯/护眼灯', 4, 2, 1),
                                                                                 ('计算器', 4, 3, 1),
                                                                                 ('打印机/耗材', 4, 4, 1),
                                                                                 ('背书神器', 4, 5, 1),  -- 背诵板/计时器等
                                                                                 ('绘图工具', 4, 6, 1);

-- 插入运动器材二级分类（parent_id=5）
INSERT INTO `item_category` (`category_name`, `parent_id`, `sort`, `status`) VALUES
                                                                                 ('篮球/足球/排球', 5, 1, 1),
                                                                                 ('羽毛球/乒乓球', 5, 2, 1),
                                                                                 ('健身器材', 5, 3, 1),  -- 瑜伽垫/哑铃/握力器等
                                                                                 ('跑步装备', 5, 4, 1),
                                                                                 ('护具', 5, 5, 1),
                                                                                 ('跳绳/毽子', 5, 6, 1);

-- 插入服饰鞋包二级分类（parent_id=6，学生适配）
INSERT INTO `item_category` (`category_name`, `parent_id`, `sort`, `status`) VALUES
                                                                                 ('日常穿搭', 6, 1, 1),
                                                                                 ('校服/院服', 6, 2, 1),  -- 专属校园
                                                                                 ('鞋子', 6, 3, 1),
                                                                                 ('背包/书包', 6, 4, 1),
                                                                                 ('配饰', 6, 5, 1),
                                                                                 ('军训服', 6, 6, 1);  -- 专属校园

-- 插入电子产品配件二级分类（parent_id=7）
INSERT INTO `item_category` (`category_name`, `parent_id`, `sort`, `status`) VALUES
                                                                                 ('电脑配件', 7, 1, 1),  -- 鼠标/键盘/散热器等
                                                                                 ('手机配件', 7, 2, 1),  -- 手机壳/膜/充电器等
                                                                                 ('平板配件', 7, 3, 1),
                                                                                 ('耳机配件', 7, 4, 1),
                                                                                 ('充电头/插排', 7, 5, 1);

-- 插入文娱周边二级分类（parent_id=8）
INSERT INTO `item_category` (`category_name`, `parent_id`, `sort`, `status`) VALUES
                                                                                 ('书籍小说', 8, 1, 1),
                                                                                 ('乐器', 8, 2, 1),  -- 吉他/尤克里里/口琴等
                                                                                 ('手办/周边', 8, 3, 1),
                                                                                 ('桌游/卡牌', 8, 4, 1),
                                                                                 ('海报/贴纸', 8, 5, 1);

-- 插入考研考公资料二级分类（parent_id=9，校园刚需）
INSERT INTO `item_category` (`category_name`, `parent_id`, `sort`, `status`) VALUES
                                                                                 ('考研网课资料', 9, 1, 1),
                                                                                 ('考公网课资料', 9, 2, 1),
                                                                                 ('真题试卷', 9, 3, 1),
                                                                                 ('冲刺押题', 9, 4, 1),
                                                                                 ('错题本/思维导图', 9, 5, 1);

-- 插入其他闲置二级分类（parent_id=10）
INSERT INTO `item_category` (`category_name`, `parent_id`, `sort`, `status`) VALUES
                                                                                 ('校园卡/水卡', 10, 1, 1),  -- 专属校园
                                                                                 ('闲置礼品', 10, 2, 1),
                                                                                 ('实验器材', 10, 3, 1),  -- 理工科专属
                                                                                 ('社团用品', 10, 4, 1),
                                                                                 ('杂项闲置', 10, 5, 1);