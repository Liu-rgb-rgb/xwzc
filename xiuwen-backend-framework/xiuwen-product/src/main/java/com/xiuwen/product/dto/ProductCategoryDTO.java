package com.xiuwen.product.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 商品分类新增/编辑请求体。
 */
@Data
public class ProductCategoryDTO {

    /** 父分类ID，为NULL表示一级分类 */
    private Long parentId;

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50个字符")
    private String name;

    /** 分类图标URL */
    @Size(max = 255, message = "图标URL长度不能超过255个字符")
    private String icon;

    /** 排序值，越小越靠前，默认0 */
    private Integer sort = 0;

    /** 分类状态：NORMAL启用 DISABLED禁用，默认NORMAL */
    private String status = "NORMAL";
}
