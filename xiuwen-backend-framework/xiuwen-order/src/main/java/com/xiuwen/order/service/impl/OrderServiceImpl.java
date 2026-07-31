package com.xiuwen.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.common.utils.IdUtil;
import com.xiuwen.order.entity.OrderDetail;
import com.xiuwen.order.entity.OrderItem;
import com.xiuwen.order.entity.Orders;
import com.xiuwen.order.mapper.OrderMapper;
import com.xiuwen.order.service.OrderItemService;
import com.xiuwen.order.service.OrderService;
import com.xiuwen.order.vo.OrderStatusCountVO;
import com.xiuwen.product.entity.CartItemDetail;
import com.xiuwen.product.entity.CustomDesignDetail;
import com.xiuwen.product.entity.Product;
import com.xiuwen.product.service.CartItemService;
import com.xiuwen.product.service.CustomDesignService;
import com.xiuwen.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * orders 表服务实现。
 */
@RequiredArgsConstructor
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    private final CartItemService cartItemService;
    private final ProductService productService;
    private final OrderItemService orderItemService;
    private final CustomDesignService customDesignService;

    // =============== 创建订单 ===============
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Orders createOrder(Long userId, Long addressId, Long[] cartItemIds,
                              Long customDesignId, Integer quantity, String remark) {

        // 收货地址（临时硬编码，等 UserAddressService 完成后替换）
        String receiverName = "待填充";    // TODO: 从地址服务获取
        String receiverPhone = "待填充";   // TODO
        String receiverAddress = "待填充"; // TODO

        // 构建订单项
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (cartItemIds != null && cartItemIds.length > 0) {
            // 购物车下单
            for (Long cartItemId : cartItemIds) {
                CartItemDetail cartItem = cartItemService.getCartItemDetail(cartItemId);
                if (cartItem == null) {
                    throw new BusinessException("购物车项不存在: " + cartItemId);
                }

                OrderItem item = new OrderItem();
                item.setProductId(cartItem.getProductId());
                item.setProductName(cartItem.getProductName());
                item.setProductImage(cartItem.getProductCoverImage());
                item.setPatternId(cartItem.getPatternId());
                item.setCustomDesignId(cartItem.getCustomDesignId());
                item.setPreviewImageUrl(cartItem.getPreviewImageUrl());
                item.setQuantity(cartItem.getQuantity());
                item.setUnitPrice(cartItem.getUnitPrice());

                BigDecimal totalPrice = cartItem.getUnitPrice()
                        .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                item.setTotalPrice(totalPrice);

                orderItems.add(item);
                totalAmount = totalAmount.add(totalPrice);
            }
        } else if (customDesignId != null) {
            // 定制方案直接下单
            CustomDesignDetail design = customDesignService.getDesignDetail(customDesignId);
            if (design == null) {
                throw new BusinessException("定制方案不存在");
            }

            Product product = productService.getById(design.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在");
            }

            int qty = quantity != null ? quantity : 1;
            BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty));

            OrderItem item = new OrderItem();
            item.setProductId(design.getProductId());
            item.setProductName(product.getName());
            item.setProductImage(product.getCoverImage());
            item.setPatternId(design.getPatternId());
            item.setCustomDesignId(customDesignId);
            item.setPreviewImageUrl(design.getPreviewImageUrl());
            item.setQuantity(qty);
            item.setUnitPrice(unitPrice);
            item.setTotalPrice(totalPrice);

            orderItems.add(item);
            totalAmount = totalAmount.add(totalPrice);
        } else {
            throw new BusinessException("请选择要下单的商品");
        }

        // 创建订单
        Orders order = new Orders();
        order.setOrderNo(IdUtil.generateOrderNo());
        order.setUserId(userId);
        order.setCustomDesignId(customDesignId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setStatus("WAIT_PAY");
        order.setPayStatus("UNPAID");
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        order.setRemark(remark);
        save(order);

        // 保存订单明细
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
        }
        orderItemService.saveBatchOrderItems(orderItems);

        // 扣减库存
        for (OrderItem item : orderItems) {
            if (item.getProductId() != null) {
                Product product = productService.getById(item.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() - item.getQuantity());
                    productService.updateById(product);
                }
            }
        }

        // 清理购物车
        if (cartItemIds != null && cartItemIds.length > 0) {
            cartItemService.removeByIds(Arrays.asList(cartItemIds));
        }

        return order;
    }

    // =============== 模拟支付 ===============
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Orders mockPay(Long orderId, Long userId) {
        Orders order = getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在或无权操作");
        }
        if (!"WAIT_PAY".equals(order.getStatus())) {
            throw new BusinessException("订单状态不允许支付");
        }

        order.setPayStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        order.setStatus("WAIT_CONFIRM");
        updateById(order);
        return order;
    }

    // =============== 我的订单列表 ===============
    @Override
    public IPage<OrderDetail> pageMyOrders(Long userId, long page, long pageSize, String status, String keyword) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Orders::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Orders::getOrderNo, keyword)
                    .or().like(Orders::getReceiverName, keyword));
        }
        wrapper.orderByDesc(Orders::getCreatedAt);

        IPage<Orders> orderPage = page(new Page<>(page, pageSize), wrapper);

        // 转换为 OrderDetail，通过 XML 查询填充订单项
        List<OrderDetail> detailList = orderPage.getRecords().stream()
                .map(order -> {
                    OrderDetail detail = baseMapper.selectOrderWithItems(order.getId());
                    if (detail != null) {
                        enrichOrderDetail(detail);
                    }
                    return detail;
                })
                .toList();

        Page<OrderDetail> result = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(detailList);
        return result;
    }

    // =============== 订单详情 ===============
    @Override
    public OrderDetail getOrderDetail(Long userId, Long orderId) {
        OrderDetail detail = baseMapper.selectOrderWithItems(orderId);
        if (detail == null || !detail.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在或无权查看");
        }
        enrichOrderDetail(detail);
        return detail;
    }

    // =============== 取消订单 ===============
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Orders cancelOrder(Long userId, Long orderId) {
        Orders order = getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在或无权操作");
        }
        if (!"WAIT_PAY".equals(order.getStatus()) && !"WAIT_CONFIRM".equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不可取消");
        }

        order.setStatus("CANCELLED");
        order.setCancelledAt(LocalDateTime.now());
        updateById(order);

        // 恢复库存
        List<OrderItem> items = orderItemService.listByOrderId(orderId);
        for (OrderItem item : items) {
            if (item.getProductId() != null) {
                Product product = productService.getById(item.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() + item.getQuantity());
                    productService.updateById(product);
                }
            }
        }

        return order;
    }

    // =============== 确认收货 ===============
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Orders confirmOrder(Long userId, Long orderId) {
        Orders order = getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在或无权操作");
        }
        if (!"DELIVERED".equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不可确认收货");
        }

        order.setStatus("COMPLETED");
        order.setCompletedAt(LocalDateTime.now());
        updateById(order);
        return order;
    }

    // =============== 订单状态数量 ===============
    @Override
    public OrderStatusCountVO getOrderStatusCount(Long userId) {
        OrderStatusCountVO vo = new OrderStatusCountVO();
        vo.setAll(countByUser(userId));
        vo.setWaitPay(countByUserAndStatus(userId, "WAIT_PAY"));
        vo.setWaitConfirm(countByUserAndStatus(userId, "WAIT_CONFIRM"));
        vo.setProducing(countByUserAndStatus(userId, "PRODUCING"));
        vo.setWaitDelivery(countByUserAndStatus(userId, "WAIT_DELIVERY"));
        vo.setDelivered(countByUserAndStatus(userId, "DELIVERED"));
        vo.setCompleted(countByUserAndStatus(userId, "COMPLETED"));
        vo.setCancelled(countByUserAndStatus(userId, "CANCELLED"));
        return vo;
    }



    private long countByUser(Long userId) {
        return count(new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, userId));
    }

    private long countByUserAndStatus(Long userId, String status) {
        return count(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getUserId, userId)
                .eq(Orders::getStatus, status));
    }

    // =============== 补充订单摘要 ===============
    private void enrichOrderDetail(OrderDetail detail) {
        List<OrderItem> items = detail.getItems();
        if (items == null || items.isEmpty()) {
            detail.setItemCount(0);
            detail.setProductSummary("");
            return;
        }
        detail.setItemCount(items.size());

        OrderItem first = items.get(0);
        int totalQty = items.stream().mapToInt(OrderItem::getQuantity).sum();
        String summary = first.getProductName();
        if (items.size() > 1) {
            summary += " 等" + items.size() + "件商品";
        } else {
            summary += " × " + totalQty;
        }
        detail.setProductSummary(summary);
        detail.setPreviewImageUrl(first.getPreviewImageUrl());
    }



    //===================================================================================================
    //======================================商家端========================================================
    //===================================================================================================

    // =============== 商家端：分页查询订单列表 ===============
    @Override
    public IPage<OrderDetail> pageAdminOrders(long page, long pageSize, String status,
                                               String keyword, String startTime, String endTime) {
        // 通过 XML 自定义 SQL 查询订单列表（含订单项摘要）
        List<OrderDetail> allRecords = baseMapper.selectAdminOrderList(status, keyword, startTime, endTime);

        // 手动分页
        int total = allRecords.size();
        int fromIndex = (int) ((page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + (int) pageSize, total);

        List<OrderDetail> pageRecords;
        if (fromIndex >= total) {
            pageRecords = List.of();
        } else {
            pageRecords = allRecords.subList(fromIndex, toIndex);
        }

        // 填充摘要信息
        pageRecords.forEach(this::enrichOrderDetail);

        Page<OrderDetail> result = new Page<>(page, pageSize, total);
        result.setRecords(pageRecords);
        return result;
    }

    // =============== 商家端：获取订单详情 ===============
    @Override
    public OrderDetail getAdminOrderDetail(Long orderId) {
        OrderDetail detail = baseMapper.selectAdminOrderDetail(orderId);
        if (detail == null) {
            throw new BusinessException("订单不存在");
        }
        enrichOrderDetail(detail);
        return detail;
    }

    // =============== 商家端：更新订单状态 ===============
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Orders updateOrderStatus(Long orderId, String targetStatus) {
        Orders order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        String currentStatus = order.getStatus();
        LocalDateTime now = LocalDateTime.now();

        // 校验状态流转合法性
        switch (targetStatus) {
            case "PRODUCING":
                if (!"WAIT_CONFIRM".equals(currentStatus)) {
                    throw new BusinessException("只有待接单状态的订单才能转为制作中");
                }
                order.setConfirmedAt(now);
                break;
            case "WAIT_DELIVERY":
                if (!"PRODUCING".equals(currentStatus)) {
                    throw new BusinessException("只有制作中状态的订单才能转为待发货");
                }
                order.setProducedAt(now);
                break;
            case "DELIVERED":
                if (!"WAIT_DELIVERY".equals(currentStatus)) {
                    throw new BusinessException("只有待发货状态的订单才能发货");
                }
                order.setShippedAt(now);
                break;
            default:
                throw new BusinessException("不支持的目标状态: " + targetStatus);
        }

        order.setStatus(targetStatus);
        updateById(order);
        return order;
    }

    // =============== 商家端：更新商家备注 ===============
    @Override
    public void updateMerchantRemark(Long orderId, String remark) {
        Orders order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setMerchantRemark(remark);
        updateById(order);
    }

    // =============== 商家端：全部订单各状态数量统计 ===============
    @Override
    public OrderStatusCountVO getAdminOrderStatusCount() {
        OrderStatusCountVO vo = new OrderStatusCountVO();
        vo.setAll(countAll());
        vo.setWaitPay(countAllByStatus("WAIT_PAY"));
        vo.setWaitConfirm(countAllByStatus("WAIT_CONFIRM"));
        vo.setProducing(countAllByStatus("PRODUCING"));
        vo.setWaitDelivery(countAllByStatus("WAIT_DELIVERY"));
        vo.setDelivered(countAllByStatus("DELIVERED"));
        vo.setCompleted(countAllByStatus("COMPLETED"));
        vo.setCancelled(countAllByStatus("CANCELLED"));
        return vo;
    }

    private long countAll() {
        return count();
    }

    private long countAllByStatus(String status) {
        return count(new LambdaQueryWrapper<Orders>().eq(Orders::getStatus, status));
    }

}
