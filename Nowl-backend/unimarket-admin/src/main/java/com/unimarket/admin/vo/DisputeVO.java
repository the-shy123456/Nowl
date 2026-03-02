package com.unimarket.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 纠纷记录VO
 */
@Data
public class DisputeVO {

    private Long recordId;

    private Long initiatorId;
    private String initiatorName;
    private String initiatorAvatar;

    private Long relatedId;
    private String relatedName;
    private String relatedAvatar;

    private Long contentId;
    private String orderNo;

    private Integer targetType;

    private String schoolCode;
    private String campusCode;
    private String schoolName;
    private String campusName;

    private String content;

    private String evidenceUrls;

    private Integer handleStatus;

    private String handleResult;

    private Integer claimSellerCreditPenalty;

    private Integer claimRefund;

    private BigDecimal claimRefundAmount;

    private Integer initiatorReplyCount;

    private Integer relatedReplyCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
