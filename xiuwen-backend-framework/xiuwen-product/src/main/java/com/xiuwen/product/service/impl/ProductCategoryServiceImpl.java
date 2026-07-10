package com.xiuwen.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.product.entity.ProductCategory;
import com.xiuwen.product.mapper.ProductCategoryMapper;
import com.xiuwen.product.service.ProductCategoryService;
import org.springframework.stereotype.Service;

/**
 * product_category 表服务实现。
 */
@Service
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory> implements ProductCategoryService {
}
