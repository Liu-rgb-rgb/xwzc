package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 商家端定制管理接口。
 */
@RestController
@RequestMapping("/api/admin/custom-designs")
public class MerchantCustomDesignController {


    @GetMapping
    public Result<Void> list() { return Result.todo("定制方案列表"); }

    @GetMapping("/{id}")
    public Result<Void> detail(@PathVariable Long id) { return Result.todo("定制方案详情"); }

    @GetMapping("/{id}/download")
    public Result<Void> download(@PathVariable Long id) { return Result.todo("下载生产图片"); }

    @GetMapping("/{customDesignId}/download")
    public Result<Void> downloadDesign(@PathVariable Long customDesignId){return Result.todo("下载定制方案图片");}
}
