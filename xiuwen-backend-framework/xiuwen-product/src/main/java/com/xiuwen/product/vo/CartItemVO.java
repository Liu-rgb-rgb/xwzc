package com.xiuwen.product.vo;

import com.baomidou.mybatisplus.annotation.*;
import com.xiuwen.product.entity.CartItem;
import com.xiuwen.product.entity.CartItemDetail;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;



/**
 * ClassName: CartItemVO
 * Package: com.xiuwen.product.vo
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/20 22:25
 * @Version 1.0
 */
@Data
public class CartItemVO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long patternId;
    private Long customDesignId;
    private Integer quantity;
    private Integer selected;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;


    private String productName;
    private String productCoverImage;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String previewImageUrl;

    public static CartItemVO from (CartItem entity) {
        if(entity == null) return null;
        CartItemVO vo = new CartItemVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
    public static CartItemVO from(CartItemDetail detail) {
        if (detail == null) return null;
        CartItemVO vo = from((CartItem) detail);
        vo.setProductName(detail.getProductName());
        vo.setProductCoverImage(detail.getProductCoverImage());
        vo.setUnitPrice(detail.getUnitPrice());
        vo.setPreviewImageUrl(detail.getPreviewImageUrl());

        // 计算小计
        if (detail.getUnitPrice() != null && detail.getQuantity() != null) {
            vo.setSubtotal(detail.getUnitPrice()
                    .multiply(BigDecimal.valueOf(detail.getQuantity())));
        }
        return vo;
    }

}
