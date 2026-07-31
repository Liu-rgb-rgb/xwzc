package com.xiuwen.product.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新增商品请求体。
 */
@Data
public class ProductCreateDTO {

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String subtitle;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.00", message = "商品价格不能小于0")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    private Integer stock;

    private String coverImage;

    private String mockupImage;

    private String description;

    /** 是否支持纹样定制：1支持 0不支持 */
    private Integer isCustomizable;

    /** 是否首页推荐：1是 0否 */
    private Integer isRecommend;

    /** 排序值，越小越靠前 */
    private Integer sort;
}
