package com.xiuwen.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiuwen.common.constant.RoleConstants;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.common.utils.Md5Utils;
import com.xiuwen.framework.security.JwtUtils;
import com.xiuwen.framework.security.LoginUser;
import com.xiuwen.system.dto.LoginRequest;
import com.xiuwen.system.dto.LoginResponse;
import com.xiuwen.system.dto.RegisterRequest;
import com.xiuwen.system.entity.User;
import com.xiuwen.system.mapper.UserMapper;
import com.xiuwen.system.service.AuthService;
import com.xiuwen.system.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务实现。
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserMapper userMapper, JwtUtils jwtUtils, BCryptPasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        String dbPassword = user.getPasswordHash();
        boolean isLoginSuccess = false;

        // 1. 旧数据：MD5（长度 32）—— 校验通过后顺手升级为 BCrypt
        if (dbPassword != null && dbPassword.length() == 32) {
            if (Md5Utils.matches(request.getPassword(), dbPassword)) {
                isLoginSuccess = true;
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                log.info("用户 {} 的密码已从 MD5 平滑升级至 BCrypt", user.getUsername());
            }
        }
        // 2. 新数据：BCrypt（长度 60）
        else if (dbPassword != null && dbPassword.length() == 60) {
            isLoginSuccess = passwordEncoder.matches(request.getPassword(), dbPassword);
        }

        if (!isLoginSuccess) {
            throw new BusinessException("密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtUtils.createToken(new LoginUser(user.getId(), user.getUsername(), user.getRole()));
        LoginResponse.UserInfo info = new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(), user.getRole());
        return new LoginResponse(token, info);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException("账号已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() == null ? request.getUsername() : request.getNickname());
        user.setPhone(request.getPhone());
        user.setRole(RoleConstants.USER);
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }


}
