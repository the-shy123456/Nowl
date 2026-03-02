package com.unimarket.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品审核消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsAuditMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 操作类型：1-新增，2-更新
     */
    private Integer operationType;

    public static final int TYPE_CREATE = 1;
    public static final int TYPE_UPDATE = 2;

    public static GoodsAuditMessage create(Long productId) {
        return GoodsAuditMessage.builder()
                .productId(productId)
                .operationType(TYPE_CREATE)
                .build();
    }

    public static GoodsAuditMessage update(Long productId) {
        return GoodsAuditMessage.builder()
                .productId(productId)
                .operationType(TYPE_UPDATE)
                .build();
    }
}
