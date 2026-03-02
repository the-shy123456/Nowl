package com.unimarket.module.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

/**
 * 订单退款申请DTO
 */
@Data
public class RefundApplyDTO {

    /**
     * 退款原因
     */
    @NotBlank(message = "退款原因不能为空")
    @Length(max = 255, message = "退款原因不能超过255个字符")
    private String reason;

    /**
     * 退款金额（不能超过订单支付金额）
     */
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    private BigDecimal amount;
}

