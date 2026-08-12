package com.xiuwen.web.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.framework.security.LoginUserHolder;
import com.xiuwen.order.dto.OrderCreateDTO;
import com.xiuwen.order.entity.OrderDetail;
import com.xiuwen.order.entity.Orders;
import com.xiuwen.order.service.OrderService;
import com.xiuwen.order.vo.OrderStatusCountVO;
import com.xiuwen.order.vo.OrderVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户端订单接口。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderUserController {

    private final OrderService orderService;

    public OrderUserController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 11.1 创建订单
     */
    @PostMapping
    public Result<OrderVO> createOrder(@Valid @RequestBody OrderCreateDTO orderCreateDTO) {
        Long userId = LoginUserHolder.getRequiredUserId();
        Orders order = orderService.createOrder(
                userId,
                orderCreateDTO.getAddressId(),
                orderCreateDTO.getCartItemIds(),
                orderCreateDTO.getCustomDesignId(),
                orderCreateDTO.getQuantity(),
                orderCreateDTO.getRemark()
        );
        return Result.success(OrderVO.from(order));
    }

    /**
     * 11.2 模拟支付
     */
    @PostMapping("/{orderId}/mock-pay")
    public Result<OrderVO> mockPay(@PathVariable Long orderId) {
        Long userId = LoginUserHolder.getRequiredUserId();
        Orders order = orderService.mockPay(orderId, userId);
        return Result.success(OrderVO.from(order));
    }

    /**
     * 11.3 我的订单列表
     */
    @GetMapping("/my")
    public Result<PageResult<OrderVO>> listMyOrders(
            @RequestParam(required = false, defaultValue = "1") long page,
            @RequestParam(required = false, defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Long userId = LoginUserHolder.getRequiredUserId();
        IPage<OrderDetail> pageResult = orderService.pageMyOrders(userId, page, pageSize, status, keyword);
        List<OrderVO> voList = pageResult.getRecords().stream().map(OrderVO::fromDetail).toList();
        return Result.success(PageResult.of(
                pageResult.getCurrent(),
                pageResult.getSize(),
                pageResult.getTotal(),
                voList
        ));
    }

    /**
     * 11.4 我的订单各状态数量
     */
    @GetMapping("/status-count")
    public Result<OrderStatusCountVO> statusCount() {
        Long userId = LoginUserHolder.getRequiredUserId();
        return Result.success(orderService.getOrderStatusCount(userId));
    }

    /**
     * 11.5 订单详情
     */
    @GetMapping("/{orderId}")
    public Result<OrderVO> detail(@PathVariable Long orderId) {
        Long userId = LoginUserHolder.getRequiredUserId();
        OrderDetail detail = orderService.getOrderDetail(userId, orderId);
        return Result.success(OrderVO.fromDetail(detail));
    }

    /**
     * 11.6 取消订单
     */
    @PutMapping("/{orderId}/cancel")
    public Result<OrderVO> cancel(@PathVariable Long orderId) {
        Long userId = LoginUserHolder.getRequiredUserId();
        Orders order = orderService.cancelOrder(userId, orderId);
        return Result.success(OrderVO.from(order));
    }

    /**
     * 11.7 确认收货
     */
    @PutMapping("/{orderId}/confirm")
    public Result<OrderVO> confirm(@PathVariable Long orderId) {
        Long userId = LoginUserHolder.getRequiredUserId();
        Orders order = orderService.confirmOrder(userId, orderId);
        return Result.success(OrderVO.from(order));
    }
}
