package com.xiuwen.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.system.entity.HomeRecommend;
import com.xiuwen.system.mapper.HomeRecommendMapper;
import com.xiuwen.system.service.HomeRecommendService;
import org.springframework.stereotype.Service;

/**
 * home_recommend 表服务实现。
 */
@Service
public class HomeRecommendServiceImpl extends ServiceImpl<HomeRecommendMapper, HomeRecommend> implements HomeRecommendService {
}
