package com.xiuwen.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 注册请求。
 */
@Data
public class RegisterRequest {
    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String nickname;
    private String phone;
}
