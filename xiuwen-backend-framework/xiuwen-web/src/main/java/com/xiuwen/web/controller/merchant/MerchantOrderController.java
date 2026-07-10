package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 商家端订单管理接口。
 */
@RestController
@RequestMapping("/api/merchant/orders")
public class MerchantOrderController {


    @GetMapping
    public Result<Void> list() { return Result.todo("商家订单列表"); }

    @GetMapping("/{id}")
    public Result<Void> detail(@PathVariable Long id) { return Result.todo("商家订单详情"); }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id) { return Result.todo("修改订单状态"); }

}
