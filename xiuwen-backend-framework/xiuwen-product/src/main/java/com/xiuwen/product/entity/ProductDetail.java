package com.xiuwen.product.entity;

import lombok.Data;

/**
 * 商品详情（含分类名称）
 * 用于ProductMapper.selectProductWithCategory查询结果映射
 */
@Data
public class ProductDetail extends Product {

    /** 分类名称 */
    private String categoryName;
}
