package com.unimarket.module.order.dto;

import com.unimarket.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderQueryDTO extends PageQuery {

    /**
     * 订单状态：0-待支付，1-待发货，2-待收货，3-已完成，4-已取消
     */
    private Integer orderStatus;

    /**
     * 订单类型：buy-我买的，sell-我卖的
     */
    private String orderType;
}
