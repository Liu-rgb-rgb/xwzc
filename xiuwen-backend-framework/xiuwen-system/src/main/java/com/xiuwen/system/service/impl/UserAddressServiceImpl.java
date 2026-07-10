package com.xiuwen.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.system.entity.UserAddress;
import com.xiuwen.system.mapper.UserAddressMapper;
import com.xiuwen.system.service.UserAddressService;
import org.springframework.stereotype.Service;

/**
 * user_address 表服务实现。
 */
@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {
}
