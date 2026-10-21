package com.xiuwen.product.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * ClassName: CartAddDTO
 * Package: com.xiuwen.product.dto
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/21 22:29
 * @Version 1.0
 */
@Data
public class CartAddDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 纹样ID，可为空 */
    private Long patternId;

    /** 定制设计ID，可为空 */
    private Long customDesignId;

    /** 数量，默认1 */
    private Integer quantity = 1;
}

