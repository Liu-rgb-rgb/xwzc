package com.xiuwen.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.system.entity.ShopInfo;
import com.xiuwen.system.mapper.ShopInfoMapper;
import com.xiuwen.system.service.ShopInfoService;
import org.springframework.stereotype.Service;

/**
 * shop_info 表服务实现。
 */
@Service
public class ShopInfoServiceImpl extends ServiceImpl<ShopInfoMapper, ShopInfo> implements ShopInfoService {
}
