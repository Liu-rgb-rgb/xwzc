package com.xiuwen.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 商家端课程新增/编辑请求。
 */
@Data
public class CourseSaveRequest {

    @NotBlank(message = "课程标题不能为空")
    @Size(max = 200, message = "课程标题不能超过200个字符")
    private String title;

    @Size(max = 200, message = "课程副标题不能超过200个字符")
    private String subtitle;

    private String coverImage;

    private BigDecimal price;

    @Size(max = 50, message = "讲师名称不能超过50个字符")
    private String teacherName;

    private String duration;

    private String difficulty;

    private String description;

    private String content;

    private String videoUrl;

    /** 是否首页推荐：1是 0否 */
    private Integer isRecommend;

    /** 排序值，越小越靠前 */
    private Integer sort;

    /** 课程状态：DRAFT/PUBLISHED/HIDDEN */
    private String status;

    /** 课程分类ID，对应course_category.id，JSON字段名为"id"与API文档保持一致 */
    @JsonProperty("id")
    private Long categoryId;
}
