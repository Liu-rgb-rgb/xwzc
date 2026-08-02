package com.xiuwen.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class BannerSaveRequest {
    @NotBlank(message = "Banner标题不能为空")
    private String title;

    @NotBlank(message = "Banner图片不能为空")
    private String imageUrl;

    private String subtitle;
    private String buttonText;
    private String linkType;
    private Long linkId;
    private String linkUrl;
    private Integer sort;
    private Integer status;
}
