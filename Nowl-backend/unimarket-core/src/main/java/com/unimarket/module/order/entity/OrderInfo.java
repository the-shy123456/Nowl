package com.unimarket.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息实体类
 */
@Data
@TableName("order_info")
public class OrderInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID（主键）
     */
    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;

    /**
     * 订单编号（唯一）
     */
    private String orderNo;

    /**
     * 买家ID
     */
    private Long buyerId;

    /**
     * 卖家ID
     */
    private Long sellerId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 学校编码（冗余字段，用于管理范围隔离）
     */
    private String schoolCode;

    /**
     * 校区编码（冗余字段，用于管理范围隔离）
     */
    private String campusCode;

    /**
     * 订单金额
     */
    private BigDecimal orderAmount;

    /**
     * 运费
     */
    private BigDecimal deliveryFee;

    /**
     * 总金额（商品金额+运费）
     */
    private BigDecimal totalAmount;

    /**
     * 订单状态：0-待支付，1-待发货，2-待收货，3-已完成，4-已取消，5-已结束
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
     * 退款处理人ID（卖家/系统）
     */
    private Long refundProcessorId;

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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
