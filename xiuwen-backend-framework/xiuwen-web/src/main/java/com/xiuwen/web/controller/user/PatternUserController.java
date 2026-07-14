package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import com.xiuwen.framework.security.LoginUserHolder;
import com.xiuwen.pattern.dto.GeneratePatternRequest;

import com.xiuwen.pattern.service.PatternGenerateService;
import com.xiuwen.pattern.service.PatternService;
import com.xiuwen.pattern.vo.GeneratePatternResponse;
import org.springframework.web.bind.annotation.*;
import com.xiuwen.pattern.dto.RegeneratePatternRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

/**
 * 用户端AI纹样接口。
 */
@RestController
@RequestMapping("/api/patterns")
public class PatternUserController {
private final PatternService patternService;
    private final PatternGenerateService patternGenerateService;

    public PatternUserController(PatternService patternService, PatternGenerateService patternGenerateService) {
		this.patternService = patternService;
        this.patternGenerateService = patternGenerateService;
    }

    @GetMapping("/options")
    public Result<Void> options() { return Result.todo("AI生成选项"); }

    @PostMapping("/generate")
    public Result<GeneratePatternResponse> generate(
            @Valid
            @RequestBody GeneratePatternRequest request) {
   Long userId = LoginUserHolder.getRequiredUserId();
    return Result.success(patternGenerateService.generate(
            userId,
            request));
}



    @PostMapping("/regenerate")
    public Result<GeneratePatternResponse> regenerate(
            @Valid @RequestBody RegeneratePatternRequest request) {
        Long userId = LoginUserHolder.getRequiredUserId();
        return Result.success(
                patternGenerateService.regenerate(userId, request)
        );
    }


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
