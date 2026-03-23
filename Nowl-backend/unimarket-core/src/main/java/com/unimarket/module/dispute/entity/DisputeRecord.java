package com.unimarket.module.dispute.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 纠纷仲裁记录实体类
 */
@Data
@TableName("dispute_record")
public class DisputeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 仲裁记录ID（主键）
     */
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    /**
     * 发起人ID
     */
    private Long initiatorId;

    /**
     * 关联人ID
     */
    private Long relatedId;

    /**
     * 关联订单/跑腿任务ID
     */
    private Long contentId;

    /**
     * 争议类型：0-商品交易，1-跑腿劳务
     */
    private Integer targetType;

    /**
     * 学校编码（用于管理范围隔离）
     */
    private String schoolCode;

    /**
     * 校区编码（用于管理范围隔离）
     */
    private String campusCode;

    /**
     * 争议内容
     */
    private String content;

    /**
     * 证据URL（JSON格式）
     */
    private String evidenceUrls;

    /**
     * 处理状态：0-待处理，1-处理中，2-已解决，3-已驳回，4-已撤回
     */
    private Integer handleStatus;

    /**
     * 处理结果
     */
    private String handleResult;

    /**
     * 买家诉求：是否申请扣除卖家信用分（0-否，1-是）
     */
    private Integer claimSellerCreditPenalty;

    /**
     * 买家诉求：是否申请退还金额（0-否，1-是）
     */
    private Integer claimRefund;

    /**
     * 申请退款金额
     */
    private BigDecimal claimRefundAmount;

    /**
     * 实际裁定退款金额
     */
    private BigDecimal resolvedRefundAmount;

    /**
     * 实际裁定扣除信用分
     */
    private Integer resolvedCreditPenalty;

    /**
     * 发起人补充次数
     */
    private Integer initiatorReplyCount;

    /**
     * 被投诉方补充次数
     */
    private Integer relatedReplyCount;

    /**
     * 双方交流记录（JSON）
     */
    private String conversationLogs;

    /**
     * 处理人ID（管理员）
     */
    private Long handlerId;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
