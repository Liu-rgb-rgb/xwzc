package com.xiuwen.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.order.entity.OrderItem;
import com.xiuwen.order.mapper.OrderItemMapper;
import com.xiuwen.order.service.OrderItemService;
import org.springframework.stereotype.Service;

/**
 * order_item 表服务实现。
 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {
}
