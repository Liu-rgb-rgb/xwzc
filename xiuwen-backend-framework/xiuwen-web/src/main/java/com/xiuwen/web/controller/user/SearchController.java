package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 全站搜索接口。
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {


    @GetMapping
    public Result<Void> search() { return Result.todo("全站搜索"); }

}
