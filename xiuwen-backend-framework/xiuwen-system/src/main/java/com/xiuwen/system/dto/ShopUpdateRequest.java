package com.xiuwen.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 修改店铺信息请求。
 */
@Data
public class ShopUpdateRequest {
    @NotBlank(message = "店铺名称不能为空")
    private String shopName;

    private String logo;
    private String slogan;
    private String contactName;
    private String contactPhone;
    private String email;
    private String address;
    private String description;
    private Integer status;
}
