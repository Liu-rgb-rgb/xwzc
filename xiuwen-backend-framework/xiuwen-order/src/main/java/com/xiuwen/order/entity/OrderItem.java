package com.xiuwen.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细表
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属订单ID */
    private Long orderId;

    /** 商品ID */
    private Long productId;

    /** 定制设计ID */
    private Long customDesignId;

    /** 商品名称快照 */
    private String productName;

    /** 商品图片快照URL */
    private String productImage;

    /** 纹样ID */
    private Long patternId;

    /** 纹样图片快照URL */
    private String patternImageUrl;

    /** 定制预览图快照URL */
    private String previewImageUrl;

    /** 购买数量 */
    private Integer quantity;

    /** 商品单价 */
    private BigDecimal unitPrice;

    /** 明细小计金额 */
    private BigDecimal totalPrice;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
