package com.xiuwen.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.product.entity.CartItem;
import com.xiuwen.product.mapper.CartItemMapper;
import com.xiuwen.product.service.CartItemService;
import org.springframework.stereotype.Service;

/**
 * cart_item 表服务实现。
 */
@Service
public class CartItemServiceImpl extends ServiceImpl<CartItemMapper, CartItem> implements CartItemService {
}
