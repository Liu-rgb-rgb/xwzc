package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.framework.security.LoginUserHolder;
import com.xiuwen.system.dto.LoginRequest;
import com.xiuwen.system.dto.LoginResponse;
import com.xiuwen.system.dto.RegisterRequest;
import com.xiuwen.system.entity.User;
import com.xiuwen.system.service.AuthService;
import com.xiuwen.system.service.UserService;
import javax.validation.Valid;

import com.xiuwen.system.vo.UserInfoVO;
import org.springframework.web.bind.annotation.*;

/**
 * 登录注册接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result<UserInfoVO> getLoginUser() {
        User user = userService.getById(LoginUserHolder.getRequiredUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        return Result.success(vo);
    }


    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }


    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {return Result.todo("退出登录");}
}
