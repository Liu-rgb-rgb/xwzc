package com.xiuwen.product.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商品状态更新请求体。
 */
@Data
public class ProductStatusUpdateDTO {

    @NotBlank(message = "状态不能为空")
    private String status;
}
