package com.xiuwen.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuwen.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * product 表 Mapper。
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
