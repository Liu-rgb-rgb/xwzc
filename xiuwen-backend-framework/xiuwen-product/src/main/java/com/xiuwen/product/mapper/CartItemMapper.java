package com.xiuwen.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuwen.product.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * cart_item 表 Mapper。
 */
@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
