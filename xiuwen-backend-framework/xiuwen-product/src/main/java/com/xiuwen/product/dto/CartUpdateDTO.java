package com.xiuwen.product.dto;

import lombok.Data;

/**
 * ClassName: CartUpdateDTO
 * Package: com.xiuwen.product.dto
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/22 11:16
 * @Version 1.0
 */
@Data
public class CartUpdateDTO {
    private Integer quantity;
    private Integer selected;
}
