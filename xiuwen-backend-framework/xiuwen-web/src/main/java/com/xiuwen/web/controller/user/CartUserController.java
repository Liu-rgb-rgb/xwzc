package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 用户端购物车接口。
 */
@RestController
@RequestMapping("/api/cart")
public class CartUserController {


    @GetMapping
    public Result<Void> list() { return Result.todo("购物车列表"); }

    @PostMapping
    public Result<Void> add() { return Result.todo("加入购物车"); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id) { return Result.todo("修改购物车数量"); }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { return Result.todo("删除购物车项"); }

    @DeleteMapping
    public Result<Void> clear() { return Result.todo("清空购物车"); }

}
