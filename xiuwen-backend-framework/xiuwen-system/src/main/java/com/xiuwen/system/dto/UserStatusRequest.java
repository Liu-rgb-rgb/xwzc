package com.xiuwen.system.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UserStatusRequest {
    @NotNull(message = "状态不能为空")
    private Integer status;
}
