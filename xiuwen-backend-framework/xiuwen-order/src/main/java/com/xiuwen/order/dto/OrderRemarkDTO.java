package com.xiuwen.order.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 商家修改订单备注请求DTO
 */
@Data
public class OrderRemarkDTO {

    /** 商家备注 */
    @NotNull(message = "备注内容不能为空")
    private String remark;
}
