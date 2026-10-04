package com.xiuwen.course.dto;

import com.xiuwen.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家端课程列表查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseQueryRequest extends PageQuery {

    /** 课程分类ID */
    private Long categoryId;

    /** 课程关键词（标题模糊搜索） */
    private String keyword;

    /** 课程难度：BEGINNER/BASIC/ADVANCED */
    private String difficulty;

    /** 课程状态：DRAFT/PUBLISHED/HIDDEN */
    private String status;
}
