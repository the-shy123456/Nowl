package com.unimarket.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI聊天消息VO
 */
@Data
public class AiChatMessageVO {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色：user/model
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 图片URL（可选）
     */
    private String imageUrl;

    /**
     * 商品卡片（仅AI消息可能存在）
     */
    private List<AiGoodsCardVO> cards;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
