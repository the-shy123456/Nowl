package com.unimarket.module.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

/**
 * 纠纷创建DTO
 */
@Data
public class DisputeCreateDTO {

    /**
     * 关联订单/跑腿任务ID
     */
    @NotNull(message = "关联ID不能为空")
    private Long contentId;

    /**
     * 争议类型：0-商品交易，1-跑腿劳务
     */
    @NotNull(message = "争议类型不能为空")
    private Integer targetType;

    /**
     * 争议内容
     */
    @NotBlank(message = "争议内容不能为空")
    @Length(max = 1000, message = "争议内容不能超过1000个字符")
    private String content;

    /**
     * 证据URL（JSON格式，多张图片）
     */
    private String evidenceUrls;

    /**
     * 买家诉求：申请扣除卖家信用分（0-否，1-是）
     */
    @NotNull(message = "请选择是否申请扣除卖家信用分")
    private Integer claimSellerCreditPenalty;

    /**
     * 买家诉求：申请退还金额（0-否，1-是）
     */
    @NotNull(message = "请选择是否申请退还金额")
    private Integer claimRefund;

    /**
     * 申请退还金额
     */
    private BigDecimal claimRefundAmount;
}
