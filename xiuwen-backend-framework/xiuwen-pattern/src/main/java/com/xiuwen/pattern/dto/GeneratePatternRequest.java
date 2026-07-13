package com.xiuwen.pattern.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * AI 纹样生成请求。
 */
@Data
public class GeneratePatternRequest {

    /**
     * 风格：
     * classic / new_chinese
     */
    @NotBlank(message = "请选择纹样风格")
    private String style;

    /**
     * 用户选择或输入的元素。
     */
    @NotEmpty(message = "请至少选择一个纹样元素")
    @Size(max = 8, message = "纹样元素最多选择8个")
    private List<String> elements;

    /**
     * 配色：
     * chinese_elegant / red_gold
     */
    @NotBlank(message = "请选择配色方案")
    private String colorTheme;

    /**
     * 应用场景：
     * product / poster
     */
    @NotBlank(message = "请选择应用场景")
    private String usageScene;

    /**
     * 灵感描述。
     */
    @Size(max = 200, message = "灵感描述不能超过200个字符")
    private String description;

    /**
     * 用户上传的参考图地址，第一版暂时可以为空。
     */
    private String referenceImageUrl;

    /**
     * 当前前端允许1到4张。
     */
    @NotNull(message = "请输入生成数量")
    @Min(value = 1, message = "生成数量不能少于1张")
    @Max(value = 4, message = "生成数量不能超过4张")
    private Integer count;
}