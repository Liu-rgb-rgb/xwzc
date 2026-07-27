package com.xiuwen.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiuwen.product.entity.Product;
import com.xiuwen.product.entity.ProductDetail;
import com.xiuwen.product.vo.ProductVO;

import java.util.List;

/**
 * product 表服务接口。
 */
public interface ProductService extends IService<Product> {

    IPage<Product> pageProducts(
            Long categoryId, String keyword,
            String sortBy, String sortOrder,
            Double minPrice, Double maxPrice,
            int page, int pageSize);

    ProductDetail getProductDetail(Long productId);

    List<Product> listRecommendProducts(Integer limit);
}
