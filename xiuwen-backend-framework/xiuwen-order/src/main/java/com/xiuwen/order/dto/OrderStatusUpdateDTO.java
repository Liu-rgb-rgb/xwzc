package com.xiuwen.order.dto;


import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 商家修改订单状态请求
 */
@Data
public class OrderStatusUpdateDTO {

    @NotNull(message = "订单状态不能为空")
    private String status;
}
