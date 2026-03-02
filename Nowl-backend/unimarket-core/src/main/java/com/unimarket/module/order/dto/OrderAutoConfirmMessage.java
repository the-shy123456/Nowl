package com.unimarket.module.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单自动确认消息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderAutoConfirmMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 发货时间戳（用于校验）
     */
    private Long deliveryTimestamp;
}
