package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 商家端用户管理接口。
 */
@RestController
@RequestMapping("/api/admin/users")
public class MerchantUserController {


    @GetMapping
    public Result<Void> list() { return Result.todo("用户列表"); }

    @GetMapping("/{id}")
    public Result<Void> detail(@PathVariable Long id) { return Result.todo("用户详情"); }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id) { return Result.todo("启用或禁用用户"); }

    @PutMapping("/{userId}/status")
    public Result<Void> userIdStatus(@PathVariable Long userId){return Result.todo("禁用或恢复用户");}

}
