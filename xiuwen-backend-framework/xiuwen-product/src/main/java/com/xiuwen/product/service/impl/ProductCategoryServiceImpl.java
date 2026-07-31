package com.xiuwen.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.product.dto.ProductCategoryDTO;
import com.xiuwen.product.entity.Product;
import com.xiuwen.product.entity.ProductCategory;
import com.xiuwen.product.mapper.ProductCategoryMapper;
import com.xiuwen.product.mapper.ProductMapper;
import com.xiuwen.product.service.ProductCategoryService;
import com.xiuwen.product.vo.ProductCategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * product_category 表服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory> implements ProductCategoryService {

    private final ProductCategoryMapper productCategoryMapper;
    private final ProductMapper productMapper;

    @Override
    public List<ProductCategoryVO> listActiveCategoryVOs() {
        List<ProductCategory> entities = list(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getStatus, "NORMAL")
                .orderByAsc(ProductCategory::getSort));
        return entities.stream().map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductCategoryVO> listCategories(String status) {
        LambdaQueryWrapper<ProductCategory> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            queryWrapper.eq(ProductCategory::getStatus, status);
        }
        queryWrapper.orderByAsc(ProductCategory::getSort)
                .orderByDesc(ProductCategory::getCreatedAt);
        List<ProductCategory> entities = list(queryWrapper);
        return entities.stream().map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductCategoryVO createCategory(ProductCategoryDTO dto) {
        // 1. 校验分类名称唯一性（同一父分类下名称不能重复）
        LambdaQueryWrapper<ProductCategory> nameQuery = new LambdaQueryWrapper<>();
        nameQuery.eq(ProductCategory::getName, dto.getName());
        if (dto.getParentId() != null) {
            nameQuery.eq(ProductCategory::getParentId, dto.getParentId());
        } else {
            nameQuery.isNull(ProductCategory::getParentId);
        }
        if (count(nameQuery) > 0) {
            throw new BusinessException("同一层级下分类名称已存在");
        }

        // 2. 校验父分类是否存在
        if (dto.getParentId() != null) {
            ProductCategory parent = getById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException("父分类不存在");
            }
        }

        // 3. 创建分类
        ProductCategory entity = new ProductCategory();
        BeanUtils.copyProperties(dto, entity);
        save(entity);

        log.info("新增商品分类成功，ID: {}, 名称: {}", entity.getId(), entity.getName());
        return convertToVO(entity);
    }

    @Override
    public ProductCategoryVO updateCategory(Long productId, ProductCategoryDTO dto) {
        // 1. 检查分类是否存在
        ProductCategory entity = getById(productId);
        if (entity == null) {
            throw new BusinessException("分类不存在");
        }

        // 2. 校验分类名称唯一性（同一父分类下名称不能重复，排除自己）
        LambdaQueryWrapper<ProductCategory> nameQuery = new LambdaQueryWrapper<>();
        nameQuery.eq(ProductCategory::getName, dto.getName());
        nameQuery.ne(ProductCategory::getId, productId);
        Long parentId = dto.getParentId() != null ? dto.getParentId() : entity.getParentId();
        if (parentId != null) {
            nameQuery.eq(ProductCategory::getParentId, parentId);
        } else {
            nameQuery.isNull(ProductCategory::getParentId);
        }
        if (count(nameQuery) > 0) {
            throw new BusinessException("同一层级下分类名称已存在");
        }

        // 3. 校验父分类是否存在
        if (dto.getParentId() != null) {
            if (dto.getParentId().equals(productId)) {
                throw new BusinessException("父分类不能是自己");
            }
            ProductCategory parent = getById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException("父分类不存在");
            }
        }

        // 4. 更新分类
        BeanUtils.copyProperties(dto, entity);
        entity.setId(productId);
        updateById(entity);

        log.info("编辑商品分类成功，ID: {}, 名称: {}", entity.getId(), entity.getName());
        return convertToVO(entity);
    }

    @Override
    public void deleteCategory(Long productId) {
        // 1. 检查分类是否存在
        ProductCategory entity = getById(productId);
        if (entity == null) {
            throw new BusinessException("分类不存在");
        }

        // 2. 检查是否存在子分类
        LambdaQueryWrapper<ProductCategory> childQuery = new LambdaQueryWrapper<>();
        childQuery.eq(ProductCategory::getParentId, productId);
        if (count(childQuery) > 0) {
            throw new BusinessException("该分类下存在子分类，无法删除");
        }

        // 3. 检查是否有关联商品
        LambdaQueryWrapper<Product> productQuery = new LambdaQueryWrapper<>();
        productQuery.eq(Product::getCategoryId, productId);
        if (productMapper.selectCount(productQuery) > 0) {
            throw new BusinessException("该分类下存在关联商品，无法删除");
        }

        // 4. 逻辑删除
        removeById(productId);
        log.info("删除商品分类成功，ID: {}, 名称: {}", entity.getId(), entity.getName());
    }

    private ProductCategoryVO convertToVO(ProductCategory entity) {
        if (entity == null) {
            return null;
        }
        ProductCategoryVO vo = new ProductCategoryVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
