package com.xiuwen.order.entity;

import lombok.Data;
import java.util.List;

/**
 * 订单详情（含订单项列表 + 关联信息）
 */
@Data
public class OrderDetail extends Orders {

    /** 订单项列表 */
    private List<OrderItem> items;

    /** 用户昵称（商家端查询时填充） */
    private String userNickname;

    /** 订单项数量 */
    private Integer itemCount;

    /** 商品摘要（如 "广绣牡丹帆布袋 × 2"） */
    private String productSummary;

    /** 定制预览图（取第一个订单项的） */
    private String previewImageUrl;
}
