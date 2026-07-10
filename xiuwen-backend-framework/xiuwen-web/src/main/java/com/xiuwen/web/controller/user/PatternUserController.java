package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import com.xiuwen.pattern.dto.GeneratePatternRequest;

import org.springframework.web.bind.annotation.*;

/**
 * 用户端AI纹样接口。
 */
@RestController
@RequestMapping("/api/patterns")
public class PatternUserController {


    @GetMapping("/options")
    public Result<Void> options() { return Result.todo("AI生成选项"); }

    @PostMapping("/generate")
    public Result<Void> generate(@RequestBody GeneratePatternRequest request) { return Result.todo("AI生成纹样"); }

    @PostMapping("/regenerate")
    public Result<Void> regenerate() { return Result.todo("重新生成纹样"); }

    @GetMapping("/my")
    public Result<Void> myPatterns() { return Result.todo("我的纹样列表"); }

    @GetMapping("/{id}")
    public Result<Void> detail(@PathVariable Long id) { return Result.todo("纹样详情"); }

    @PostMapping("/{id}/save")
    public Result<Void> save(@PathVariable Long id) { return Result.todo("保存纹样"); }

    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id) { return Result.todo("收藏纹样"); }

    @DeleteMapping("/{id}/favorite")
    public Result<Void> cancelFavorite(@PathVariable Long id) { return Result.todo("取消收藏纹样"); }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { return Result.todo("删除纹样"); }

    @GetMapping("/{id}/download")
    public Result<Void> download(@PathVariable Long id) { return Result.todo("下载纹样"); }

}
