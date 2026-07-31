package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.product.dto.ProductCategoryDTO;
import com.xiuwen.product.dto.ProductCreateDTO;
import com.xiuwen.product.dto.ProductQueryDTO;
import com.xiuwen.product.dto.ProductStatusUpdateDTO;
import com.xiuwen.product.entity.Product;
import com.xiuwen.product.entity.ProductDetail;
import com.xiuwen.product.service.ProductCategoryService;
import com.xiuwen.product.service.ProductService;
import com.xiuwen.product.vo.ProductCategoryVO;
import com.xiuwen.product.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 商家端商品管理接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class MerchantProductController {

    private final ProductService productService;
    private final ProductCategoryService productCategoryService;

    // 3.==================== 商品分类管理 ====================

    /**
     * 商品分类列表（支持状态筛选）
     */
    @GetMapping("/categories")
    public Result<List<ProductCategoryVO>> listCategories(
            @RequestParam(required = false) String status) {
        List<ProductCategoryVO> voList = productCategoryService.listCategories(status);
        return Result.success(voList);
    }

    /**
     * 新增商品分类
     */
    @PostMapping("/categories")
    public Result<ProductCategoryVO> addCategory(
            @Valid @RequestBody ProductCategoryDTO dto) {
        ProductCategoryVO vo = productCategoryService.createCategory(dto);
        return Result.success(vo);
    }

    /**
     * 编辑商品分类
     */
    @PutMapping("/categories/{categoryId}")
    public Result<ProductCategoryVO> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody ProductCategoryDTO dto) {
        ProductCategoryVO vo = productCategoryService.updateCategory(categoryId, dto);
        return Result.success(vo);
    }

    /**
     * 删除商品分类
     */
    @DeleteMapping("/categories/{categoryId}")
    public Result<Void> deleteCategory(
            @PathVariable Long categoryId) {
        productCategoryService.deleteCategory(categoryId);
        return Result.success();
    }

    //4. =============================================== 商品管理 ==================================================

    /**
     * 4.1 商品列表（分页，支持关键词/分类/状态/推荐筛选）
     */
    @GetMapping
    public Result<PageResult<ProductVO>> list(ProductQueryDTO query) {
        PageResult<ProductVO> page = productService.pageAdminProducts(query);
        return Result.success(page);
    }

    /**
     * 4.2 新增商品
     */
    @PostMapping
    public Result<ProductVO> create(@Valid @RequestBody ProductCreateDTO dto) {
        ProductVO vo = productService.createProduct(dto);
        return Result.success(vo);
    }

    /**
     * 4.3 商品详情
     */
    @GetMapping("/{productId}")
    public Result<ProductVO> getDetail(@PathVariable Long productId) {
        Product product = productService.getById(productId);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        ProductVO vo = ProductVO.from(product);
        ProductDetail detail = productService.getProductDetail(productId);
        if (detail != null) {
            vo.setCategoryName(detail.getCategoryName());
        }
        return Result.success(vo);
    }

    /**
     * 4.4 编辑商品
     */
    @PutMapping("/{productId}")
    public Result<ProductVO> update(@PathVariable Long productId,
                                    @Valid @RequestBody ProductCreateDTO dto) {
        log.info("编辑商品: productId={}, dto={}", productId, dto);
        Product product = productService.getById(productId);
        if (product == null) {
            return Result.fail("商品不存在");
        }
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
        product.setSort(dto.getSort() != null ? dto.getSort() : 0);
        productService.updateById(product);
        return Result.success(ProductVO.from(product));
    }

    /**
     * 4.5 商品上下架
     */
    @PutMapping("/{productId}/status")
    public Result<ProductVO> status(@PathVariable Long productId,
                                    @Valid @RequestBody ProductStatusUpdateDTO dto) {
        Product product = productService.getById(productId);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        product.setStatus(dto.getStatus());
        productService.updateById(product);
        return Result.success(ProductVO.from(product));
    }

    /**
     * 4.6 删除商品（逻辑删除）
     */
    @DeleteMapping("/{productId}")
    public Result<Void> delete(@PathVariable Long productId) {
        productService.removeById(productId);
        return Result.success();
    }

}
