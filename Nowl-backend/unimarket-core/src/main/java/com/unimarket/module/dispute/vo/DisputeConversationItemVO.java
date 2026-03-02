package com.unimarket.module.dispute.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 纠纷交流记录项
 */
@Data
public class DisputeConversationItemVO {

    /**
     * 回复人ID
     */
    private Long userId;

    /**
     * 回复人昵称
     */
    private String userName;

    /**
     * 回复人头像
     */
    private String userAvatar;

    /**
     * 是否发起人
     */
    private Boolean initiator;

    /**
     * 回复文本
     */
    private String content;

    /**
     * 回复附加证据URL
     */
    private List<String> evidenceUrls;

    /**
     * 回复时间
     */
    private LocalDateTime createTime;
}

