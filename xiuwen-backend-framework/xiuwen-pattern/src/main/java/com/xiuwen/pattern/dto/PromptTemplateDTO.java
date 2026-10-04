package com.xiuwen.pattern.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 提示词模板创建/更新请求对象
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptTemplateDTO {

    @NotBlank(message = "模板名称不能为空")
    private String name;

    @NotBlank(message = "适用风格不能为空")
    private String style;

    private String usageScene;

    private String colorTheme;

    @NotBlank(message = "模板内容不能为空")
    private String templateText;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private Integer sort;
}
