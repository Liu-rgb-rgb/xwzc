package com.xiuwen.web.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;

import com.xiuwen.product.entity.Product;
import com.xiuwen.product.entity.ProductDetail;
import com.xiuwen.product.service.ProductCategoryService;
import com.xiuwen.product.service.ProductService;
import com.xiuwen.product.vo.ProductCategoryVO;
import com.xiuwen.product.vo.ProductDetailVO;
import com.xiuwen.product.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户端文创商品接口。
 */

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductUserController {

private final ProductCategoryService productCategoryService;
private final ProductService productService;
//=================商品分类列表====================
    @GetMapping("/categories")
    public Result<List<ProductCategoryVO>> listCategories() {
    List<ProductCategoryVO> voList = productCategoryService.listActiveCategoryVOs();
        return Result.success(voList);
    }
//========================商品列表===========================
    @GetMapping
    public Result<PageResult<ProductVO>> listProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "sort") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "12") int pageSize) {
        IPage<Product> pageResult = productService.pageProducts(
                categoryId, keyword, sortBy, sortOrder, minPrice, maxPrice, page, pageSize);

        List<ProductVO> voList = pageResult.getRecords().stream()
                .map(ProductVO::from)
                .toList();


        return Result.success(
                PageResult.of(
                        pageResult.<ProductVO>getTotal(),
                        (int)pageResult.getCurrent(),
                        (int)pageResult.getSize(),
                        voList));

    }
//===========================商品详情==========================
    @GetMapping("/{productId}")
    public Result<ProductDetailVO> detailProduct(@PathVariable Long productId) {
        ProductDetail product = productService.getProductDetail(productId);
        if (product == null) {
            return Result.fail(404, "商品不存在");
        }
        return Result.success(ProductDetailVO.from(product));
    }
//========================推荐商品列表============================
    @GetMapping("/recommends")
    public Result<List<ProductVO>> listRecommendProducts(
            @RequestParam(required = false, defaultValue = "8") Integer limit) {
        List<Product> products = productService.listRecommendProducts(limit);
        List<ProductVO> voList = products.stream()
                .map(ProductVO::from)
                .toList();
        return Result.success(voList);
    }
}
