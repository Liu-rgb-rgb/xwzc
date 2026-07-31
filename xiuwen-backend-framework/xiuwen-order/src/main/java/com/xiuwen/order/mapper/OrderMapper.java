package com.xiuwen.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuwen.order.entity.OrderDetail;
import com.xiuwen.order.entity.OrderStatusCount;
import com.xiuwen.order.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    /**
     * 查询订单详情（含订单项列表）
     */
    OrderDetail selectOrderWithItems(@Param("id") Long orderId);

    /**
     * 查询订单列表（含摘要信息）
     */
    List<OrderDetail> selectOrderListWithSummary(@Param("userId") Long userId,
                                                  @Param("status") String status,
                                                  @Param("keyword") String keyword);

    /**
     * 统计用户各状态订单数量
     */
    OrderStatusCount selectStatusCount(@Param("userId") Long userId);

    /**
     * 商家端：查询订单列表（含摘要 + 用户昵称）
     */
    List<OrderDetail> selectAdminOrderList(@Param("status") String status,
                                            @Param("keyword") String keyword,
                                            @Param("startTime") String startTime,
                                            @Param("endTime") String endTime);

    /**
     * 商家端：查询单个订单详情（含订单项，不做用户权限校验）
     */
    OrderDetail selectAdminOrderDetail(@Param("id") Long orderId);
}
