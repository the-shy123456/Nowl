package com.unimarket.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 卖家处理退款DTO
 */
@Data
public class RefundProcessDTO {

    /**
     * 处理动作：approve/reject
     */
    @NotBlank(message = "处理动作不能为空")
    private String action;

    /**
     * 处理备注
     */
    @Length(max = 255, message = "处理备注不能超过255个字符")
    private String remark;
}

