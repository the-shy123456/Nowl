package com.unimarket.module.dispute.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 纠纷详情VO
 */
@Data
public class DisputeDetailVO {

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
     * 争议内容
     */
    private String content;

    /**
     * 证据URL列表
     */
    private List<String> evidenceUrlList;

    /**
     * 处理状态：0-待处理，1-处理中，2-已解决，3-已驳回，4-已撤回
     */
    private Integer handleStatus;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 处理结果
     */
    private String handleResult;

    /**
     * 买家诉求：申请扣除卖家信用分（0-否，1-是）
     */
    private Integer claimSellerCreditPenalty;

    /**
     * 买家诉求：申请退还金额（0-否，1-是）
     */
    private Integer claimRefund;

    /**
     * 申请退还金额
     */
    private BigDecimal claimRefundAmount;

    /**
     * 发起人补充次数
     */
    private Integer initiatorReplyCount;

    /**
     * 被投诉方补充次数
     */
    private Integer relatedReplyCount;

    /**
     * 是否还能补充（最多3次）
     */
    private Boolean canReply;

    /**
     * 双方交流记录（按时间顺序）
     */
    private List<DisputeConversationItemVO> conversations;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 是否是发起人
     */
    private Boolean isInitiator;

    /**
     * 是否可撤回
     */
    private Boolean canWithdraw;

    // ========== 发起人信息 ==========
    /**
     * 发起人ID
     */
    private Long initiatorId;

    /**
     * 发起人昵称
     */
    private String initiatorName;

    /**
     * 发起人头像
     */
    private String initiatorAvatar;

    // ========== 被投诉方信息 ==========
    /**
     * 被投诉方ID
     */
    private Long relatedId;

    /**
     * 被投诉方昵称
     */
    private String relatedName;

    /**
     * 被投诉方头像
     */
    private String relatedAvatar;

    // ========== 关联订单信息（targetType=0时有值） ==========
    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 商品标题
     */
    private String productTitle;

    /**
     * 商品图片
     */
    private String productImage;

    /**
     * 订单金额
     */
    private BigDecimal orderAmount;

    /**
     * 订单状态
     */
    private Integer orderStatus;

    // ========== 关联跑腿信息（targetType=1时有值） ==========
    /**
     * 跑腿任务标题
     */
    private String errandTitle;

    /**
     * 跑腿任务图片
     */
    private String errandImage;

    /**
     * 跑腿报酬
     */
    private BigDecimal errandReward;

    /**
     * 跑腿任务状态
     */
    private Integer errandStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
