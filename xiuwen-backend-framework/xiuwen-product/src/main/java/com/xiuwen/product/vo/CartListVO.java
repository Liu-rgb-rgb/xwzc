package com.xiuwen.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * ClassName: CartListVO
 * Package: com.xiuwen.product.vo
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/20 22:25
 * @Version 1.0
 */
@Data
public class CartListVO {
    private List<CartItemVO>  items;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
}
