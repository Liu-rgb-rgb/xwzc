package com.xiuwen.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuwen.product.entity.Product;
import com.xiuwen.product.entity.ProductDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * product 表 Mapper。
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT p.*, pc.name AS categoryName FROM product p " +
            "LEFT JOIN product_category pc ON p.category_id = pc.id " +
            "WHERE p.id = #{productId} AND p.deleted = 0")
    ProductDetail selectProductWithCategory(@Param("productId") Long productId);
}
