package com.xiuwen.product.dto;

import lombok.Data;

/**
 * 商品列表查询参数。
 */
@Data
public class ProductQueryDTO {

    /** 商品ID/名称关键词 */
    private String keyword;

    /** 分类ID */
    private Long categoryId;

    /** 商品状态：ON_SALE/OFF_SALE/SOLD_OUT/DRAFT */
    private String status;

    /** 是否推荐：0否 1是 */
    private Integer isRecommend;

    /** 页码，从1开始 */
    private int page = 1;

    /** 每页条数 */
    private int pageSize = 10;
}
