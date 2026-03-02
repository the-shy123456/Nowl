package com.unimarket.recommend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户偏好画像实体
 */
@Data
@TableName("user_preference")
public class UserPreference {

    /**
     * 用户ID
     */
    @TableId(type = IdType.INPUT)
    private Long userId;

    /**
     * 分类偏好 JSON格式
     * 例如: {"1":0.8,"3":0.5,"5":0.3}
     * key为分类ID, value为偏好分数(0-1)
     */
    private String categoryScores;

    /**
     * 价格偏好 JSON格式
     * 例如: {"min":50,"max":300,"avg":120}
     */
    private String pricePreference;

    /**
     * 行为统计 JSON格式
     * 例如: {"view":100,"collect":20,"buy":5}
     */
    private String behaviorCount;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
