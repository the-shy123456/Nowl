package com.unimarket.module.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 订单创建DTO
 */
@Data
public class OrderCreateDTO {

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 订单备注
     */
    @Length(max = 200, message = "订单备注不能超过200个字符")
    private String remark;
}
