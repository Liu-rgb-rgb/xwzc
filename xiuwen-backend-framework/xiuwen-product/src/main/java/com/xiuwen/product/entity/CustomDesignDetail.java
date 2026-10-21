package com.xiuwen.product.entity;

import lombok.Data;

/**
 * 定制方案详情（含关联商品和纹样信息）
 * 用于CustomDesignMapper.selectDesignWithDetails查询结果映射
 */
@Data
public class CustomDesignDetail extends CustomDesign {

    /** 商品名称 */
    private String productName;

    /** 商品封面图 */
    private String productCoverImage;

    /** 纹样标题 */
    private String patternTitle;

    /** 纹样图片URL */
    private String patternImageUrl;

    /** 用户昵称 */
    private String userNickname;
}
