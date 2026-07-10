package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 商家端课程管理接口。
 */
@RestController
@RequestMapping("/api/merchant/courses")
public class MerchantCourseController {


    @GetMapping("/categories")
    public Result<Void> categories() { return Result.todo("课程分类列表"); }

    @PostMapping("/categories")
    public Result<Void> addCategory() { return Result.todo("新增课程分类"); }

    @GetMapping
    public Result<Void> list() { return Result.todo("课程列表"); }

    @PostMapping
    public Result<Void> create() { return Result.todo("新增课程"); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id) { return Result.todo("编辑课程"); }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { return Result.todo("删除课程"); }

}
