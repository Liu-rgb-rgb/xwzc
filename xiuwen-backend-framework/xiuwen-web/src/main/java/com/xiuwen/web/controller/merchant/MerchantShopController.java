package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.framework.security.LoginUserHolder;
import com.xiuwen.system.dto.ShopUpdateRequest;
import com.xiuwen.system.entity.ShopInfo;
import com.xiuwen.system.service.ShopInfoService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 商家端店铺设置接口。
 */
@RestController
@RequestMapping("/api/admin/shop")
public class MerchantShopController {

    private final ShopInfoService shopInfoService;

    public MerchantShopController(ShopInfoService shopInfoService) {
        this.shopInfoService = shopInfoService;
    }

    /** [13.1] 获取店铺信息 */
    @GetMapping
    public Result<ShopInfo> getInfo() {
        Long userId = LoginUserHolder.getRequiredUserId();
        ShopInfo shop = shopInfoService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ShopInfo>()
                        .eq(ShopInfo::getOwnerUserId, userId));
        if (shop == null) {
            throw new BusinessException("店铺不存在，请先创建店铺");
        }
        return Result.success(shop);
    }

    /** [13.2] 修改店铺信息 */
    @PutMapping
    public Result<ShopInfo> updateInfo(@Valid @RequestBody ShopUpdateRequest request) {
        Long userId = LoginUserHolder.getRequiredUserId();
        ShopInfo existing = shopInfoService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ShopInfo>()
                        .eq(ShopInfo::getOwnerUserId, userId));
        if (existing == null) {
            throw new BusinessException("店铺不存在，请先创建店铺");
        }
        ShopInfo shop = new ShopInfo();
        BeanUtils.copyProperties(request, shop);
        shop.setId(existing.getId());
        shopInfoService.updateById(shop);
        ShopInfo updated = shopInfoService.getById(existing.getId());
        return Result.success(updated);
    }
}
