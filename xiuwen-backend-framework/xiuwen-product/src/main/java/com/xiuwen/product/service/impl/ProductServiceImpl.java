package com.xiuwen.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.product.entity.Product;
import com.xiuwen.product.mapper.ProductMapper;
import com.xiuwen.product.service.ProductService;
import org.springframework.stereotype.Service;

/**
 * product 表服务实现。
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
}
