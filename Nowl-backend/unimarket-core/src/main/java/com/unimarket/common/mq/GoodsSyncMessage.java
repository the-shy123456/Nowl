package com.unimarket.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品同步消息 (共享模型)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsSyncMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 同步类型
     */
    private SyncType type;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 热度分（用于UPDATE_HOT_SCORE）
     */
    private Double hotScore;

    /**
     * 浏览量（用于UPDATE_VIEW_COUNT）
     */
    private Integer viewCount;

    /**
     * 同步类型枚举
     */
    public enum SyncType {
        CREATE,           // 新增商品
        UPDATE,           // 更新商品
        DELETE,           // 删除商品
        UPDATE_HOT_SCORE, // 更新热度分
        UPDATE_VIEW_COUNT // 更新浏览量
    }

    public static GoodsSyncMessage createMessage(Long productId) {
        return GoodsSyncMessage.builder()
                .type(SyncType.CREATE)
                .productId(productId)
                .build();
    }

    public static GoodsSyncMessage updateMessage(Long productId) {
        return GoodsSyncMessage.builder()
                .type(SyncType.UPDATE)
                .productId(productId)
                .build();
    }

    public static GoodsSyncMessage deleteMessage(Long productId) {
        return GoodsSyncMessage.builder()
                .type(SyncType.DELETE)
                .productId(productId)
                .build();
    }
}
