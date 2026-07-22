package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 商家端商品管理接口。
 */
@RestController
@RequestMapping("/api/admin/products")
public class MerchantProductController {


    @GetMapping("/categories")
    public Result<Void> categories() { return Result.todo("商品分类管理列表"); }

    @PostMapping("/categories")
    public Result<Void> addCategory() { return Result.todo("新增商品分类"); }

    @GetMapping
    public Result<Void> list() { return Result.todo("商品管理列表"); }

    @PostMapping
    public Result<Void> create() { return Result.todo("新增商品"); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id) { return Result.todo("编辑商品"); }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id) { return Result.todo("商品上下架"); }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { return Result.todo("删除商品"); }

}
