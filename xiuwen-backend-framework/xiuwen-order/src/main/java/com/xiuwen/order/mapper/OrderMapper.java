package com.xiuwen.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuwen.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * orders 表 Mapper。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
