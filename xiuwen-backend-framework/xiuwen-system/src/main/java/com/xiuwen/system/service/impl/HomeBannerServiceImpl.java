package com.xiuwen.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.system.entity.HomeBanner;
import com.xiuwen.system.mapper.HomeBannerMapper;
import com.xiuwen.system.service.HomeBannerService;
import org.springframework.stereotype.Service;

/**
 * home_banner 表服务实现。
 */
@Service
public class HomeBannerServiceImpl extends ServiceImpl<HomeBannerMapper, HomeBanner> implements HomeBannerService {
}
