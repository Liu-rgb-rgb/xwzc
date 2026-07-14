package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 用户端文创商品接口。
 */
@RestController
@RequestMapping("/api/products")
public class ProductUserController {


    @GetMapping("/categories")
    public Result<Void> categories() { return Result.todo("商品分类"); }

    @GetMapping
    public Result<Void> list() { return Result.todo("商品列表"); }

    @GetMapping("/{id}")
    public Result<Void> detail(@PathVariable Long id) { return Result.todo("商品详情"); }

    @GetMapping("/recommends")
    public Result<Void> recommends(){return Result.todo("推荐商品");}
}
