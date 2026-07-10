package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 用户个人中心接口。
 */
@RestController
@RequestMapping("/api/user")
public class UserProfileController {


    @GetMapping("/profile")
    public Result<Void> profile() { return Result.todo("获取当前用户资料"); }

    @PutMapping("/profile")
    public Result<Void> updateProfile() { return Result.todo("修改个人资料"); }

    @PutMapping("/password")
    public Result<Void> updatePassword() { return Result.todo("修改密码"); }

    @PostMapping("/avatar")
    public Result<Void> uploadAvatar() { return Result.todo("上传头像"); }

    @GetMapping("/addresses")
    public Result<Void> addressList() { return Result.todo("收货地址列表"); }

    @PostMapping("/addresses")
    public Result<Void> addAddress() { return Result.todo("新增收货地址"); }

    @PutMapping("/addresses/{id}")
    public Result<Void> updateAddress(@PathVariable Long id) { return Result.todo("修改收货地址"); }

    @DeleteMapping("/addresses/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id) { return Result.todo("删除收货地址"); }

}
