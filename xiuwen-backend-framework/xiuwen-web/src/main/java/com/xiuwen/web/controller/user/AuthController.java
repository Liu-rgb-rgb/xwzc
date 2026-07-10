package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import com.xiuwen.system.dto.LoginRequest;
import com.xiuwen.system.dto.LoginResponse;
import com.xiuwen.system.dto.RegisterRequest;
import com.xiuwen.system.service.AuthService;
import javax.validation.Valid;

import org.springframework.web.bind.annotation.*;

/**
 * 登录注册接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

}
