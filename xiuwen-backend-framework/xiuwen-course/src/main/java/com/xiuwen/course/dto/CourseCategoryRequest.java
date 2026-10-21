package com.xiuwen.course.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 课程分类新增/编辑请求。
 */
@Data
public class CourseCategoryRequest {

    /** 父分类ID，为NULL表示一级分类 */
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过50个字符")
    private String name;

    @Size(max = 255, message = "分类说明不能超过255个字符")
    private String description;

    /** 排序值，越小越靠前 */
    private Integer sort;

    /** 分类状态：1启用 0禁用 */
    private Integer status;
}
