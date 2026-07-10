package com.xiuwen.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.product.entity.CustomDesign;
import com.xiuwen.product.mapper.CustomDesignMapper;
import com.xiuwen.product.service.CustomDesignService;
import org.springframework.stereotype.Service;

/**
 * custom_design 表服务实现。
 */
@Service
public class CustomDesignServiceImpl extends ServiceImpl<CustomDesignMapper, CustomDesign> implements CustomDesignService {
}
