package com.xiuwen.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 商家端课程状态切换请求。
 */
@Data
public class CourseStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;

    /** 课程ID（路径参数已传递，此字段作为兼容保留），JSON字段名为"id"与API文档保持一致 */
    @JsonProperty("id")
    private Long courseId;
}
