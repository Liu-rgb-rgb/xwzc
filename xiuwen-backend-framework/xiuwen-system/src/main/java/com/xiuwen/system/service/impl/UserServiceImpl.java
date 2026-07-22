package com.xiuwen.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.system.entity.User;
import com.xiuwen.system.mapper.UserMapper;
import com.xiuwen.system.service.UserService;
import org.springframework.stereotype.Service;

/**
 * user 表服务实现。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
