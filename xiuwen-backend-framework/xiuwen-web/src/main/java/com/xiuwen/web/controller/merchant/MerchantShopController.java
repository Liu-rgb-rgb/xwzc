package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 商家端店铺设置接口。
 */
@RestController
@RequestMapping("/api/merchant/shop")
public class MerchantShopController {


    @GetMapping
    public Result<Void> info() { return Result.todo("获取店铺信息"); }

    @PutMapping
    public Result<Void> update() { return Result.todo("修改店铺信息"); }

}
