package com.xiuwen.pattern.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class RegeneratePatternRequest {
    @NotNull(message = "原生成记录 ID 不能为空")
    private Long generationId;

    @Size(max = 255, message = "纹样主题关键词不能超过 255 个字符")
    private String keyword;

    private String style;

    @Size(max = 8, message = "纹样元素最多选择 8 个")
    private List<String> elements;

    private String colorTheme;
    private String usageScene;

    @Size(max = 500, message = "补充描述不能超过 500 个字符")
    private String description;

    @Min(value = 1, message = "生成数量不能少于 1 张")
    @Max(value = 4, message = "生成数量不能超过 4 张")
    private Integer generateCount;
}
