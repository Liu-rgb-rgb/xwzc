package com.xiuwen.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.order.entity.Order;
import com.xiuwen.order.mapper.OrderMapper;
import com.xiuwen.order.service.OrderService;
import org.springframework.stereotype.Service;

/**
 * orders 表服务实现。
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
}
