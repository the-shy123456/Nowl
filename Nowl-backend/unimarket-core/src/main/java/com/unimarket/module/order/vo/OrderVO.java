package com.unimarket.module.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单VO
 */
@Data
public class OrderVO {

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 买家ID
     */
    private Long buyerId;

    /**
     * 买家昵称
     */
    private String buyerName;

    /**
     * 买家头像
     */
    private String buyerAvatar;

    /**
     * 卖家ID
     */
    private Long sellerId;

    /**
     * 卖家昵称
     */
    private String sellerName;

    /**
     * 卖家头像
     */
    private String sellerAvatar;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 学校编码
     */
    private String schoolCode;

    /**
     * 校区编码
     */
    private String campusCode;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 校区名称
     */
    private String campusName;

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
     * 运费
     */
    private BigDecimal deliveryFee;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 订单状态
     */
    private Integer orderStatus;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 发货时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 收货时间
     */
    private LocalDateTime receiveTime;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 退款状态：0-无退款，1-待处理，2-已退款，3-已拒绝
     */
    private Integer refundStatus;

    /**
     * 退款申请原因
     */
    private String refundReason;

    /**
     * 退款申请金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款申请时间
     */
    private LocalDateTime refundApplyTime;

    /**
     * 退款处理截止时间
     */
    private LocalDateTime refundDeadline;

    /**
     * 退款处理时间
     */
    private LocalDateTime refundProcessTime;

    /**
     * 退款处理备注
     */
    private String refundProcessRemark;

    /**
     * 退款是否走极速通道：0-否，1-是
     */
    private Integer refundFastTrack;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否存在进行中的纠纷（待处理/处理中）
     */
    private Boolean hasActiveDispute;

    /**
     * 进行中纠纷ID
     */
    private Long activeDisputeId;

    /**
     * 进行中纠纷状态：0-待处理，1-处理中
     */
    private Integer activeDisputeStatus;

    /**
     * 最近一次已处理纠纷ID
     */
    private Long latestClosedDisputeId;

    /**
     * 最近一次已处理纠纷状态：2-已解决，3-已驳回
     */
    private Integer latestClosedDisputeStatus;

    /**
     * 最近一次已处理纠纷结果摘要
     */
    private String latestClosedDisputeResult;

    /**
     * 最近一次已处理纠纷实际退款金额
     */
    private BigDecimal latestClosedDisputeRefundAmount;

    /**
     * 最近一次已处理纠纷实际扣除信用分
     */
    private Integer latestClosedDisputeCreditPenalty;
}
