package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 用户端订单接口。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderUserController {


    @PostMapping
    public Result<Void> create() { return Result.todo("创建订单"); }

    @PostMapping("/{id}/mock-pay")
    public Result<Void> mockPay(@PathVariable Long id) { return Result.todo("模拟支付"); }

    @GetMapping("/my")
    public Result<Void> myOrders() { return Result.todo("我的订单列表"); }

    @GetMapping("/{id}")
    public Result<Void> detail(@PathVariable Long id) { return Result.todo("订单详情"); }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) { return Result.todo("取消订单"); }

    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) { return Result.todo("确认收货"); }

    @GetMapping("/status-count")
    public Result<Void> statusCount(){return Result.todo("我的订单状态");}
}
