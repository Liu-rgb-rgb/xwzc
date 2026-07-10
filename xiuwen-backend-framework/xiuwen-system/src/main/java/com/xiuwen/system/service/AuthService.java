package com.xiuwen.system.service;

import com.xiuwen.system.dto.LoginRequest;
import com.xiuwen.system.dto.LoginResponse;
import com.xiuwen.system.dto.RegisterRequest;

/**
 * 认证服务。
 */
public interface AuthService {
    LoginResponse login(LoginRequest request);
    Long register(RegisterRequest request);
}
