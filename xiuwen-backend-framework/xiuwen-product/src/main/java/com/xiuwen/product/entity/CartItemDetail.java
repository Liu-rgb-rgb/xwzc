package com.xiuwen.product.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * ClassName: CartItemDetail
 * Package: com.xiuwen.product.entity
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/20 17:57
 * @Version 1.0
 */
@Data
public class CartItemDetail extends CartItem{
    private Long customDesignId;
    private String productName;
    private String productCoverImage;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String previewImageUrl;
    private Integer stock;
}
