package com.unimarket.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 跑腿任务审核消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrandAuditMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 跑腿任务ID
     */
    private Long taskId;

    /**
     * 操作类型：1-新增，2-更新
     */
    private Integer operationType;

    public static final int TYPE_CREATE = 1;
    public static final int TYPE_UPDATE = 2;

    public static ErrandAuditMessage create(Long taskId) {
        return ErrandAuditMessage.builder()
                .taskId(taskId)
                .operationType(TYPE_CREATE)
                .build();
    }

    public static ErrandAuditMessage update(Long taskId) {
        return ErrandAuditMessage.builder()
                .taskId(taskId)
                .operationType(TYPE_UPDATE)
                .build();
    }
}
