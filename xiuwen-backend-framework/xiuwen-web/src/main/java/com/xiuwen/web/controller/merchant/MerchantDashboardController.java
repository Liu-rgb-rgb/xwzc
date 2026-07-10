package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 商家端工作台接口。
 */
@RestController
@RequestMapping("/api/merchant/dashboard")
public class MerchantDashboardController {


    @GetMapping
    public Result<Void> dashboard() { return Result.todo("商家工作台数据概览"); }

    @GetMapping("/trend")
    public Result<Void> trend() { return Result.todo("近7日趋势数据"); }

}
