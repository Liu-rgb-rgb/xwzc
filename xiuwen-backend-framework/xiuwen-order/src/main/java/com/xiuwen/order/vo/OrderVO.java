package com.xiuwen.order.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.xiuwen.order.entity.OrderDetail;
import com.xiuwen.order.entity.OrderItem;
import com.xiuwen.order.entity.Orders;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应VO
 */
@Data
public class OrderVO {

    @JsonAlias("orderId")
    private Long id;
    private String orderNo;
    private Long userId;
    private Long customDesignId;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private String status;
    private String payStatus;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    private LocalDateTime paidAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime producedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 摘要信息 */
    private Integer itemCount;
    private String productSummary;
    private String previewImageUrl;
    private String userNickname;

    /** 订单项列表（详情接口返回） */
    private List<OrderItemVO> items;

    /**
     * 从基础订单实体转换
     */
    public static OrderVO from(Orders order) {
        if (order == null) return null;
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setCustomDesignId(order.getCustomDesignId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setPayStatus(order.getPayStatus());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setRemark(order.getRemark());
        vo.setPaidAt(order.getPaidAt());
        vo.setConfirmedAt(order.getConfirmedAt());
        vo.setProducedAt(order.getProducedAt());
        vo.setShippedAt(order.getShippedAt());
        vo.setCompletedAt(order.getCompletedAt());
        vo.setCancelledAt(order.getCancelledAt());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setUpdatedAt(order.getUpdatedAt());
        return vo;
    }

    /**
     * 从订单详情实体转换（含订单项和摘要）
     */
    public static OrderVO fromDetail(OrderDetail detail) {
        if (detail == null) return null;
        OrderVO vo = from((Orders) detail);
        vo.setItemCount(detail.getItemCount());
        vo.setProductSummary(detail.getProductSummary());
        vo.setPreviewImageUrl(detail.getPreviewImageUrl());
        vo.setUserNickname(detail.getUserNickname());

        // 转换订单项
        if (detail.getItems() != null) {
            vo.setItems(detail.getItems().stream()
                    .map(OrderItemVO::from)
                    .toList());
        }
        return vo;
    }

    /**
     * 订单项VO
     */
    @Data
    public static class OrderItemVO {
        private Long id;
        private Long orderId;
        private Long productId;
        private Long customDesignId;
        private String productName;
        private String productImage;
        private Long patternId;
        private String patternImageUrl;
        private String previewImageUrl;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        public static OrderItemVO from(OrderItem item) {
            if (item == null) return null;
            OrderItemVO vo = new OrderItemVO();
            vo.setId(item.getId());
            vo.setOrderId(item.getOrderId());
            vo.setProductId(item.getProductId());
            vo.setCustomDesignId(item.getCustomDesignId());
            vo.setProductName(item.getProductName());
            vo.setProductImage(item.getProductImage());
            vo.setPatternId(item.getPatternId());
            vo.setPatternImageUrl(item.getPatternImageUrl());
            vo.setPreviewImageUrl(item.getPreviewImageUrl());
            vo.setQuantity(item.getQuantity());
            vo.setUnitPrice(item.getUnitPrice());
            vo.setTotalPrice(item.getTotalPrice());
            return vo;
        }
    }
}
