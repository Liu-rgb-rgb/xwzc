package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 首页接口。
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {


    @GetMapping
    public Result<Void> home() { return Result.todo("首页聚合数据"); }

    @GetMapping("/banners")
    public Result<Void> banners() { return Result.todo("首页Banner列表"); }

    @GetMapping("/recommend")
    public Result<Void> recommend() { return Result.todo("首页推荐内容"); }

}
