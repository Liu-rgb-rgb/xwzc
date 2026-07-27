package com.xiuwen.product.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.xiuwen.common.utils.JsonObjectToStringDeserializer;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * ClassName: CustomDesignDTO
 * Package: com.xiuwen.product.dto
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/20 10:32
 * @Version 1.0
 */
@Data
public class CustomDesignDTO {
    @NotNull(message = "商品id不能为空")
    private Long productId;
    @NotNull(message = "纹样id不能为空")
    private Long patternId;
    private String remark;
    @JsonDeserialize(using = JsonObjectToStringDeserializer.class)
    private String designConfig;

}
