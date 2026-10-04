package com.xiuwen.pattern.dto;

import lombok.Data;

/**
 * 商家端纹样列表查询参数。
 */
@Data
public class PatternAdminQueryDTO {

    /** 所属用户 ID */
    private Long userId;

    /** 关键词（匹配标题或生成关键词） */
    private String keyword;

    /** 纹样风格 */
    private String style;

    /** 纹样状态：NORMAL / HIDDEN */
    private String status;

    /** 是否推荐：0否 1是 */
    private Integer isRecommend;

    /** 页码，默认 1 */
    private int page = 1;

    /** 每页数量，默认 10 */
    private int pageSize = 10;
}
