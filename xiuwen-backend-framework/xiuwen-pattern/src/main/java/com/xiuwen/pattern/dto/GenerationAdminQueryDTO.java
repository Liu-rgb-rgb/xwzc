package com.xiuwen.pattern.dto;

import lombok.Data;

/**
 * 商家端 AI 纹样生成记录列表查询参数。
 */
@Data
public class GenerationAdminQueryDTO {

    /** 所属用户 ID */
    private Long userId;

    /** 关键词（匹配生成关键词或用户描述） */
    private String keyword;

    /** 纹样风格：classic / new_chinese 等 */
    private String style;

    /** 生成状态：PENDING / SUCCESS / FAILED */
    private String status;

    /** 开始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;

    /** 页码，默认 1 */
    private int page = 1;

    /** 每页数量，默认 10 */
    private int pageSize = 10;
}
