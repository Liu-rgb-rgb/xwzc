package com.xiuwen.product.vo;

import com.xiuwen.product.entity.Product;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项响应
 */
@Data
public class ProductVO {

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

    public static ProductVO from(Product entity) {
        if (entity == null) return null;
        ProductVO vo = new ProductVO();
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
