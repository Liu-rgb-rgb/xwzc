package com.xiuwen.web.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.system.entity.ShopInfo;
import com.xiuwen.system.service.ShopInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop")
public class ShopPublicController {

    private final ShopInfoService shopInfoService;

    public ShopPublicController(ShopInfoService shopInfoService) {
        this.shopInfoService = shopInfoService;
    }

    /** [5.5] 前台店铺基础信息 */
    @GetMapping("/info")
    public Result<ShopInfo> info() {
        ShopInfo shop = shopInfoService.getOne(
                new LambdaQueryWrapper<ShopInfo>()
                        .eq(ShopInfo::getStatus, 1)
                        .last("LIMIT 1"));
        return Result.success(shop);
    }
}
