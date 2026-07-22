package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 用户端商品定制接口。
 */
@RestController
@RequestMapping("/api/custom-designs")
public class CustomDesignUserController {


    @PostMapping
    public Result<Void> create() { return Result.todo("创建定制预览"); }

    @GetMapping("/my")
    public Result<Void> myList() { return Result.todo("我的定制列表"); }

    @GetMapping("/{id}")
    public Result<Void> detail(@PathVariable Long id) { return Result.todo("定制详情"); }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { return Result.todo("删除定制方案"); }

    @DeleteMapping("/{customDesignId}")
    public Result<Void> deleteCustomDesignId(@PathVariable Long customDesignId){return Result.todo("删除我的定制");}
}
