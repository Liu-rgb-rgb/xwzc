package com.xiuwen.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 收货地址新增/修改请求。
 */
@Data
public class AddressSaveRequest {
    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    @NotBlank(message = "联系电话不能为空")
    private String receiverPhone;

    private String province;
    private String city;
    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

    private Integer isDefault;
}
