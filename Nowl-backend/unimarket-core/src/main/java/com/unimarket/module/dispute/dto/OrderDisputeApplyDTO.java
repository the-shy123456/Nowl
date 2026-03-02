package com.unimarket.module.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单纠纷申请参数
 */
@Data
public class OrderDisputeApplyDTO {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotBlank(message = "纠纷原因不能为空")
    private String reason;

    /**
     * 证据图片URL，逗号分隔
     */
    private String evidenceImages;
}

