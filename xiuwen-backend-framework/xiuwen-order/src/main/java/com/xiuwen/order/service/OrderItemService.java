package com.xiuwen.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiuwen.order.entity.OrderItem;

import java.util.List;

/**
 * order_item 表服务接口。
 */
public interface OrderItemService extends IService<OrderItem> {

    /**
     * 根据订单ID获取订单明细列表
     */
    List<OrderItem> listByOrderId(Long orderId);

    /**
     * 批量保存订单明细（自定义方法，避免和 IService.saveBatch 冲突）
     */
    void saveBatchOrderItems(List<OrderItem> items);
}
