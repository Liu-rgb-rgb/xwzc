package com.xiuwen.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class RecommendSaveRequest {
    @NotBlank(message = "推荐类型不能为空")
    private String recommendType;

    @NotNull(message = "关联内容ID不能为空")
    private Long relatedId;

    private String title;
    private String coverImage;
    private String description;
    private Integer sort;
    private Integer status;
    private String startAt;
    private String endAt;
}
