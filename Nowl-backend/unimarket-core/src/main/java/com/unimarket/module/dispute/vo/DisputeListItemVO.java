package com.unimarket.module.dispute.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 纠纷列表项VO
 */
@Data
public class DisputeListItemVO {

    /**
     * 纠纷记录ID
     */
    private Long recordId;

    /**
     * 争议类型：0-商品交易，1-跑腿劳务
     */
    private Integer targetType;

    /**
     * 争议类型描述
     */
    private String targetTypeDesc;

    /**
     * 关联订单/跑腿任务ID
     */
    private Long contentId;

    /**
     * 关联标题（商品标题/跑腿任务标题）
     */
    private String contentTitle;

    /**
     * 关联图片
     */
    private String contentImage;

    /**
     * 争议内容摘要
     */
    private String contentSummary;

    /**
     * 处理状态：0-待处理，1-处理中，2-已解决，3-已驳回，4-已撤回
     */
    private Integer handleStatus;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 对方用户ID
     */
    private Long otherUserId;

    /**
     * 对方用户昵称
     */
    private String otherUserName;

    /**
     * 对方用户头像
     */
    private String otherUserAvatar;

    /**
     * 是否是发起人
     */
    private Boolean isInitiator;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
