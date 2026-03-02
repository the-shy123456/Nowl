package com.unimarket.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 跑腿任务同步消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrandSyncMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private SyncType type;

    private Long taskId;

    public enum SyncType {
        CREATE,
        UPDATE,
        DELETE
    }

    public static ErrandSyncMessage createMessage(Long taskId) {
        return ErrandSyncMessage.builder()
                .type(SyncType.CREATE)
                .taskId(taskId)
                .build();
    }

    public static ErrandSyncMessage updateMessage(Long taskId) {
        return ErrandSyncMessage.builder()
                .type(SyncType.UPDATE)
                .taskId(taskId)
                .build();
    }

    public static ErrandSyncMessage deleteMessage(Long taskId) {
        return ErrandSyncMessage.builder()
                .type(SyncType.DELETE)
                .taskId(taskId)
                .build();
    }
}
