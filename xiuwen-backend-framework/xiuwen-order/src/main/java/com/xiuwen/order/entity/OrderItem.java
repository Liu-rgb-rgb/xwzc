package com.xiuwen.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * order_item 表实体。
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    private Integer deleted;
    private Long orderId;
    private Long productId;
    private String productName;
    private String productImage;
    private Long patternId;
    private String patternImageUrl;
    private String previewImageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
}
