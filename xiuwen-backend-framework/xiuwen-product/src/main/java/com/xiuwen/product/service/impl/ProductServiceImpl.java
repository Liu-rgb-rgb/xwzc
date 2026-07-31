package com.xiuwen.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.xiuwen.common.utils.StringUtils;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.product.dto.ProductCreateDTO;
import com.xiuwen.product.dto.ProductQueryDTO;
import com.xiuwen.product.entity.Product;
import com.xiuwen.product.entity.ProductDetail;
import com.xiuwen.product.mapper.ProductMapper;
import com.xiuwen.product.service.ProductService;
import com.xiuwen.product.vo.ProductVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * product 表服务实现。
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public IPage<Product> pageProducts(Long categoryId, String keyword, String sortBy, String sortOrder, Double minPrice, Double maxPrice, int page, int pageSize) {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        // 只查上架商品
        queryWrapper.eq(Product::getStatus, "ON_SALE");
        if(categoryId!=null){
            queryWrapper.eq(Product::getCategoryId,categoryId);
        }
        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(Product::getName, keyword)
                    .or().like(Product::getSubtitle, keyword));
        }
        if(minPrice!=null){
            queryWrapper.ge(Product::getPrice, minPrice);
        }
        if(maxPrice!=null){
            queryWrapper.le(Product::getPrice, maxPrice);
        }
        if ("sales".equals(sortBy)) {
            if ("desc".equalsIgnoreCase(sortOrder)) {
                queryWrapper.orderByDesc(Product::getSalesCount);
            } else {
                queryWrapper.orderByAsc(Product::getSalesCount);
            }
        } else if ("price".equals(sortBy)) {
            if ("desc".equalsIgnoreCase(sortOrder)) {
                queryWrapper.orderByDesc(Product::getPrice);
            } else {
                queryWrapper.orderByAsc(Product::getPrice);
            }
        } else {
            // 默认按排序值+创建时间
            queryWrapper.orderByAsc(Product::getSort)
                    .orderByDesc(Product::getCreatedAt);
        }

        return page(new Page<>(page, pageSize), queryWrapper);
    }

    @Override
    public ProductDetail getProductDetail(Long productId) {
        return baseMapper.selectProductWithCategory(productId);
    }

    @Override
    public List<Product> listRecommendProducts(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 8;
        }
        return list(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, "ON_SALE")
                .eq(Product::getIsRecommend, 1)
                .orderByAsc(Product::getSort)
                .last("LIMIT " + limit));
    }

    // =============== 商家端方法实现 ===============

    @Override
    public PageResult<ProductVO> pageAdminProducts(ProductQueryDTO query) {
        Page<Object> page = new Page<>(query.getPage(), query.getPageSize());
        IPage<ProductDetail> detailPage = baseMapper.selectAdminPage(
                page,
                query.getKeyword(),
                query.getCategoryId(),
                query.getStatus(),
                query.getIsRecommend()
        );

        List<ProductVO> voList = detailPage.getRecords().stream()
                .map(detail -> {
                    ProductVO vo = ProductVO.from((Product) detail);
                    vo.setCategoryName(detail.getCategoryName());
                    return vo;
                })
                .toList();

        return PageResult.of(detailPage.getTotal(),
                (int) detailPage.getCurrent(),
                (int) detailPage.getSize(),
                voList);
    }

    @Override
    public ProductVO createProduct(ProductCreateDTO dto) {
        Product product = new Product();
        product.setCategoryId(dto.getCategoryId());
        product.setName(dto.getName());
        product.setSubtitle(dto.getSubtitle());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCoverImage(dto.getCoverImage());
        product.setMockupImage(dto.getMockupImage());
        product.setDescription(dto.getDescription());
        product.setIsCustomizable(dto.getIsCustomizable() != null ? dto.getIsCustomizable() : 1);
        product.setIsRecommend(dto.getIsRecommend() != null ? dto.getIsRecommend() : 0);
        product.setSalesCount(0);
        product.setSort(dto.getSort() != null ? dto.getSort() : 0);
        product.setStatus("DRAFT");

        save(product);

        return ProductVO.from(product);
    }
}
