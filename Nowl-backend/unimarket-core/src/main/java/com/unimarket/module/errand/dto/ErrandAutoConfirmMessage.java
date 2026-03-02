package com.unimarket.module.errand.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 跑腿自动确认消息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrandAutoConfirmMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 送达时间戳（用于校验防止重复消费）
     */
    private Long deliverTimestamp;
}
