package com.xiuwen.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 创作资源新增/编辑请求。
 */
@Data
public class ResourceSaveRequest {

    @NotBlank(message = "资源标题不能为空")
    @Size(max = 100, message = "资源标题不能超过100个字符")
    private String title;

    @Size(max = 150, message = "资源副标题不能超过150个字符")
    private String subtitle;

    private String coverImage;

    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    private String resourceUrl;

    private String content;

    private BigDecimal price;

    /** 是否首页推荐：1是 0否 */
    private Integer isRecommend;

    /** 排序值，越小越靠前 */
    private Integer sort;

    /** 状态：DRAFT/PUBLISHED/HIDDEN */
    private String status;

    /** 关联课程ID，对应course.id，JSON字段名为"id"与API文档保持一致 */
    @JsonProperty("id")
    private Long courseId;
}
