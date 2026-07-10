package com.xiuwen.common.constant;

/**
 * 订单状态常量。
 */
public interface OrderStatus {
    /** 待支付 */
    String WAIT_PAY = "WAIT_PAY";

    /** 待商家接单 */
    String WAIT_CONFIRM = "WAIT_CONFIRM";

    /** 制作中 */
    String PRODUCING = "PRODUCING";

    /** 待发货 */
    String WAIT_DELIVERY = "WAIT_DELIVERY";

    /** 已发货 */
    String DELIVERED = "DELIVERED";

    /** 已完成 */
    String COMPLETED = "COMPLETED";

    /** 已取消 */
    String CANCELLED = "CANCELLED";
}
