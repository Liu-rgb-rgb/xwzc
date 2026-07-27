package com.xiuwen.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiuwen.product.entity.ProductCategory;
import com.xiuwen.product.vo.ProductCategoryVO;

import java.util.List;

/**
 * product_category 表服务接口。
 */
public interface ProductCategoryService extends IService<ProductCategory> {
    List<ProductCategoryVO> listActiveCategoryVOs();
}
