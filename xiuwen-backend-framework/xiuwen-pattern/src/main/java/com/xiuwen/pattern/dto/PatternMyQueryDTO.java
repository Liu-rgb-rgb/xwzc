package com.xiuwen.pattern.dto;

import lombok.Data;

/**
 * ClassName: PatternMyQueryDTO
 * Package: com.xiuwen.pattern.dto
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/16 14:37
 * @Version 1.0
 */
@Data
public class PatternMyQueryDTO {
    private String tab = "all";
    private Integer page = 1;
    private Integer pageSize = 10;
    private String style;
    private String keyword;
    private Long userId;
}
