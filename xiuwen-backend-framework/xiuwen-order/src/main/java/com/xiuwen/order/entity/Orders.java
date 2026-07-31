package com.xiuwen.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表
 */
@Data
@TableName("orders")
public class Orders {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号，业务唯一 */
    private String orderNo;

    /** 下单用户ID */
    private Long userId;

    /** 订单关联的定制设计ID */
    private Long customDesignId;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 订单状态：WAIT_PAY待支付 WAIT_CONFIRM待接单 PRODUCING制作中 WAIT_DELIVERY待发货 DELIVERED已发货 COMPLETED已完成 CANCELLED已取消 */
    private String status;

    /** 支付状态：UNPAID未支付 PAID已支付 REFUNDED已退款 */
    private String payStatus;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人手机号 */
    private String receiverPhone;

    /** 完整收货地址 */
    private String receiverAddress;

    /** 用户订单备注 */
    private String remark;

    /** 商家备注 */
    private String merchantRemark;

    /** 支付时间 */
    private LocalDateTime paidAt;

    /** 商家接单时间 */
    private LocalDateTime confirmedAt;

    /** 制作完成时间 */
    private LocalDateTime producedAt;

    /** 发货时间 */
    private LocalDateTime shippedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 取消时间 */
    private LocalDateTime cancelledAt;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
