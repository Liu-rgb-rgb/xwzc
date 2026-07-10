package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 商家端首页管理接口。
 */
@RestController
@RequestMapping("/api/merchant/home")
public class MerchantHomeManageController {


    @GetMapping("/banners")
    public Result<Void> banners() { return Result.todo("Banner管理列表"); }

    @PostMapping("/banners")
    public Result<Void> addBanner() { return Result.todo("新增Banner"); }

    @PutMapping("/banners/{id}")
    public Result<Void> updateBanner(@PathVariable Long id) { return Result.todo("编辑Banner"); }

    @DeleteMapping("/banners/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) { return Result.todo("删除Banner"); }

    @GetMapping("/recommend")
    public Result<Void> recommendList() { return Result.todo("首页推荐位列表"); }

    @PostMapping("/recommend")
    public Result<Void> addRecommend() { return Result.todo("新增推荐内容"); }

    @DeleteMapping("/recommend/{id}")
    public Result<Void> deleteRecommend(@PathVariable Long id) { return Result.todo("取消推荐内容"); }

}
