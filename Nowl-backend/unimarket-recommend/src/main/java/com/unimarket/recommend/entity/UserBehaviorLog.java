package com.unimarket.recommend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为日志实体
 */
@Data
@TableName("user_behavior_log")
public class UserBehaviorLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 行为类型: 1浏览 2收藏 3购买 4搜索
     */
    private Integer behaviorType;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 分类ID（冗余，加速推荐计算）
     */
    private Integer categoryId;

    /**
     * 搜索关键词（behaviorType=4时使用）
     */
    private String keyword;

    /**
     * 浏览时长（秒）
     */
    private Integer duration;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 行为类型枚举
     */
    public static class BehaviorType {
        public static final int VIEW = 1;     // 浏览
        public static final int COLLECT = 2;  // 收藏
        public static final int BUY = 3;      // 购买
        public static final int SEARCH = 4;   // 搜索
    }

    /**
     * 行为权重
     */
    public static class BehaviorWeight {
        public static final double VIEW = 1.0;
        public static final double COLLECT = 3.0;
        public static final double BUY = 5.0;
    }
}
