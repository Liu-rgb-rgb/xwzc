package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 用户端非遗课堂接口。
 */
@RestController
@RequestMapping("/api/courses")
public class CourseUserController {


    @GetMapping("/categories")
    public Result<Void> categories() { return Result.todo("课程分类"); }

    @GetMapping
    public Result<Void> list() { return Result.todo("课程列表"); }

    @GetMapping("/{id}")
    public Result<Void> detail(@PathVariable Long id) { return Result.todo("课程详情"); }

    @PostMapping("/{id}/study")
    public Result<Void> study(@PathVariable Long id) { return Result.todo("开始学习"); }

}
