package com.xiuwen.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.product.entity.ProductCategory;
import com.xiuwen.product.mapper.ProductCategoryMapper;
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
    @Override
    //===============商品分类列表==============================
    public List<ProductCategoryVO> listActiveCategoryVOs() {
        List<ProductCategory> entities = list(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getStatus,1)
                .orderByAsc(ProductCategory::getSort));
        return entities.stream().map(this::convertToVO)
                .collect(Collectors.toList());

    }
    private ProductCategoryVO convertToVO(ProductCategory entity) {
        if(entity == null){
            return null;
        }
        ProductCategoryVO vo = new ProductCategoryVO();
        BeanUtils.copyProperties(entity,vo);
        return vo;
    }











}
