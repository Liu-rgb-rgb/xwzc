package com.xiuwen.order.dto;


import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 创建订单请求
 */
@Data
public class OrderCreateDTO {

    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;

    /** 购物车项ID列表（购物车下单时传） */
    private Long[] cartItemIds;

    /** 定制设计ID（直接定制下单时传） */
    private Long customDesignId;

    /** 数量（直接定制下单时传） */
    private Integer quantity;

    /** 订单备注 */
    private String remark;
}
