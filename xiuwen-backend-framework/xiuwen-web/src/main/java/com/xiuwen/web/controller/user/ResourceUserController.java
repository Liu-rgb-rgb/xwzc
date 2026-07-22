package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 用户端创作资源接口。
 */
@RestController
@RequestMapping("/api/resources")
public class ResourceUserController {


    @GetMapping
    public Result<Void> list() { return Result.todo("创作资源列表"); }

    @GetMapping("/{id}")
    public Result<Void> detail(@PathVariable Long id) { return Result.todo("资源详情"); }

    @GetMapping("/{id}/download")
    public Result<Void> download(@PathVariable Long id) { return Result.todo("资源下载"); }

}
