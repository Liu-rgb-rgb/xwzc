package com.xiuwen.product.vo;

import com.xiuwen.product.entity.Product;
import com.xiuwen.product.entity.ProductDetail;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品详情响应（含分类名称）
 */
@Data
public class ProductDetailVO {

    private Long id;
    private Long categoryId;
    private String name;
    private String subtitle;
    private BigDecimal price;
    private Integer stock;
    private String coverImage;
    private String mockupImage;
    private String description;
    private Integer isCustomizable;
    private Integer isRecommend;
    private Integer salesCount;
    private Integer sort;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String categoryName;

    /**
     * 从详情实体转换（含categoryName）
     */
    public static ProductDetailVO from(ProductDetail detail) {
        if (detail == null) return null;
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(detail.getId());
        vo.setCategoryId(detail.getCategoryId());
        vo.setName(detail.getName());
        vo.setSubtitle(detail.getSubtitle());
        vo.setPrice(detail.getPrice());
        vo.setStock(detail.getStock());
        vo.setCoverImage(detail.getCoverImage());
        vo.setMockupImage(detail.getMockupImage());
        vo.setDescription(detail.getDescription());
        vo.setIsCustomizable(detail.getIsCustomizable());
        vo.setIsRecommend(detail.getIsRecommend());
        vo.setSalesCount(detail.getSalesCount());
        vo.setSort(detail.getSort());
        vo.setStatus(detail.getStatus());
        vo.setCreatedAt(detail.getCreatedAt());
        vo.setUpdatedAt(detail.getUpdatedAt());
        vo.setCategoryName(detail.getCategoryName());
        return vo;
    }

    /**
     * 从基础实体转换（不含categoryName）
     */
    public static ProductDetailVO from(Product entity) {
        if (entity == null) return null;
        if (entity instanceof ProductDetail detail) {
            return from(detail);
        }
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(entity.getId());
        vo.setCategoryId(entity.getCategoryId());
        vo.setName(entity.getName());
        vo.setSubtitle(entity.getSubtitle());
        vo.setPrice(entity.getPrice());
        vo.setStock(entity.getStock());
        vo.setCoverImage(entity.getCoverImage());
        vo.setMockupImage(entity.getMockupImage());
        vo.setDescription(entity.getDescription());
        vo.setIsCustomizable(entity.getIsCustomizable());
        vo.setIsRecommend(entity.getIsRecommend());
        vo.setSalesCount(entity.getSalesCount());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
