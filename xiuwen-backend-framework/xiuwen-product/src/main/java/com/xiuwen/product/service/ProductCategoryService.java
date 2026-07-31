package com.xiuwen.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiuwen.product.dto.ProductCategoryDTO;
import com.xiuwen.product.entity.ProductCategory;
import com.xiuwen.product.vo.ProductCategoryVO;

import java.util.List;

/**
 * product_category 表服务接口。
 */
public interface ProductCategoryService extends IService<ProductCategory> {

    /**
     * 查询启用状态的分类列表（用户端使用）。
     */
    List<ProductCategoryVO> listActiveCategoryVOs();

    /**
     * 查询分类列表，支持按状态筛选（商家端使用）。
     *
     * @param status 状态筛选，null表示查询所有状态
     * @return 分类列表
     */
    List<ProductCategoryVO> listCategories(String status);

    /**
     * 新增商品分类。
     *
     * @param dto 分类信息
     * @return 新增的分类
     */
    ProductCategoryVO createCategory(ProductCategoryDTO dto);

    /**
     * 编辑商品分类。
     *
     * @param id  分类ID
     * @param dto 分类信息
     * @return 更新后的分类
     */
    ProductCategoryVO updateCategory(Long productId, ProductCategoryDTO dto);

    /**
     * 删除商品分类（逻辑删除）。
     * 删除前会检查是否存在子分类或关联商品。
     *
     * @param id 分类ID
     */
    void deleteCategory(Long productId);
}
