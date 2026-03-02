package com.unimarket.module.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价记录实体类
 */
@Data
@TableName("review_record")
public class ReviewRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评价ID（主键）
     */
    @TableId(value = "review_id", type = IdType.AUTO)
    private Long reviewId;

    /**
     * 关联订单ID（商品交易时填写）
     */
    private Long orderId;

    /**
     * 关联跑腿任务ID（跑腿时填写）
     */
    private Long taskId;

    /**
     * 类型：0-商品交易，1-跑腿任务
     */
    private Integer targetType;

    /**
     * 评价人ID
     */
    private Long reviewerId;

    /**
     * 被评价人ID
     */
    private Long reviewedId;

    /**
     * 评分：1-5星
     */
    private Integer rating;

    /**
     * 评价内容
     */
    private String content;

    /**
     * 是否匿名：0-否，1-是
     */
    private Integer anonymous;

    /**
     * 信用分变化值
     */
    private Integer creditChange;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
