package com.unimarket.module.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 创建评价DTO
 */
@Data
public class ReviewCreateDTO {

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
    @NotNull(message = "评价类型不能为空")
    private Integer targetType;

    /**
     * 被评价人ID
     */
    @NotNull(message = "被评价人不能为空")
    private Long reviewedId;

    /**
     * 评分：1-5星
     */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1星")
    @Max(value = 5, message = "评分最高5星")
    private Integer rating;

    /**
     * 评价内容
     */
    @Length(max = 500, message = "评价内容不能超过500字")
    private String content;

    /**
     * 是否匿名：0-否，1-是
     */
    private Integer anonymous = 0;
}
