package com.xiuwen.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiuwen.order.entity.OrderDetail;
import com.xiuwen.order.entity.Orders;
import com.xiuwen.order.vo.OrderStatusCountVO;

import javax.validation.constraints.NotNull;

/**
 * orders 表服务接口。
 */
public interface OrderService extends IService<Orders> {

    /** 创建订单 */
    Orders createOrder(Long userId, Long addressId, Long[] cartItemIds,
                       Long customDesignId, Integer quantity, String remark);

    /** 模拟支付 */
    Orders mockPay(Long orderId, Long userId);

    /** 我的订单列表 */
    IPage<OrderDetail> pageMyOrders(Long userId, long page, long pageSize, String status, String keyword);

    /** 订单详情 */
    OrderDetail getOrderDetail(Long userId, Long orderId);

    /** 取消订单 */
    Orders cancelOrder(Long userId, Long orderId);

    /** 确认收货 */
    Orders confirmOrder(Long userId, Long orderId);

    /** 我的订单各状态数量 */
    OrderStatusCountVO getOrderStatusCount(Long userId);

    // ======================== 商家端方法 ========================

    /**
     * 商家端：分页查询订单列表
     */
    IPage<OrderDetail> pageAdminOrders(long page, long pageSize, String status,
                                        String keyword, String startTime, String endTime);

    /**
     * 商家端：获取订单详情（不做用户权限校验）
     */
    OrderDetail getAdminOrderDetail(Long orderId);

    /**
     * 商家端：更新订单状态（含状态流转校验）
     */
    Orders updateOrderStatus(Long orderId, String targetStatus);

    /**
     * 商家端：更新商家备注
     */
    void updateMerchantRemark(Long orderId, String remark);

    /**
     * 商家端：全部订单各状态数量统计
     */
    OrderStatusCountVO getAdminOrderStatusCount();
}
