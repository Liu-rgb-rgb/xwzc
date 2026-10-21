package com.xiuwen.order.vo;

import lombok.Data;

/**
 * 订单各状态数量
 */
@Data
public class OrderStatusCountVO {
    private Long all;
    private Long waitPay;
    private Long waitConfirm;
    private Long producing;
    private Long waitDelivery;
    private Long delivered;
    private Long completed;
    private Long cancelled;
}
