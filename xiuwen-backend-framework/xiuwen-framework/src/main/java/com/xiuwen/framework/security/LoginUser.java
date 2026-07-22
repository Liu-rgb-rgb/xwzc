package com.xiuwen.framework.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 当前登录用户简要信息。
 * JWT 解析后会转换成该对象并放入 LoginUserHolder。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 用户角色：USER / MERCHANT_ADMIN / ADMIN */
    private String role;
}
