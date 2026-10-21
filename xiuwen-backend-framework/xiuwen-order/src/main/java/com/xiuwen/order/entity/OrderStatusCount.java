package com.xiuwen.order.entity;

import lombok.Data;

/**
 * 订单各状态数量统计
 */
@Data
public class OrderStatusCount {
    private Integer all;
    private Integer waitPay;
    private Integer waitConfirm;
    private Integer producing;
    private Integer waitDelivery;
    private Integer delivered;
    private Integer completed;
    private Integer cancelled;
}
