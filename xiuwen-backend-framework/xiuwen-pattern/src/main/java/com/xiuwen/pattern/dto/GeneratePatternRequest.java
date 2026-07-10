package com.xiuwen.pattern.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 纹样生成请求。
 */
@Data
public class GeneratePatternRequest {
    private String style;
    private List<String> elements;
    private String colorTheme;
    private String usageScene;
    private String description;
    private String referenceImageUrl;
    private Integer count;
}
