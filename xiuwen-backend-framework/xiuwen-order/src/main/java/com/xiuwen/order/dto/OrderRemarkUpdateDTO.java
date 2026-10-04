package com.xiuwen.order.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 商家修改订单备注请求
 */
@Data
public class OrderRemarkUpdateDTO {

    @NotNull(message = "备注内容不能为空")
    private String remark;
}
