package com.xiuwen.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.order.entity.OrderItem;
import com.xiuwen.order.mapper.OrderItemMapper;
import com.xiuwen.order.service.OrderItemService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * order_item 表服务实现。
 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {

    @Override
    public List<OrderItem> listByOrderId(Long orderId) {
        return list(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
    }

    @Override
    public void saveBatchOrderItems(List<OrderItem> items) {
        saveBatch(items);
    }
}
