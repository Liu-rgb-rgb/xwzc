package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import com.xiuwen.framework.security.LoginUserHolder;
import com.xiuwen.pattern.dto.GeneratePatternRequest;

import com.xiuwen.pattern.dto.PatternMyQueryDTO;
import com.xiuwen.pattern.service.PatternGenerateService;
import com.xiuwen.pattern.service.PatternService;
import com.xiuwen.pattern.vo.GeneratePatternResponse;
import com.xiuwen.pattern.vo.PatternMyVO;
import org.apache.catalina.security.SecurityUtil;
import org.springframework.web.bind.annotation.*;
import com.xiuwen.pattern.dto.RegeneratePatternRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;
import java.util.Map;

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

//我的纹样
    @GetMapping("/my")
    public Result<Map<String,Object>> getMyPatterns(PatternMyQueryDTO queryDTO) {
        Long userId = LoginUserHolder.getRequiredUserId();
        queryDTO.setUserId(userId);
        Map<String,Object> data = patternService.getMyPatterns(queryDTO);
        return Result.success(data);
    }

//    @GetMapping("/{id}")
//    public Result<Void> detail(@PathVariable Long id) { return Result.todo("纹样详情"); }
//
//    @PostMapping("/{id}/save")
//    public Result<Void> save(@PathVariable Long id) { return Result.todo("保存纹样"); }
//
//    @PostMapping("/{id}/favorite")
//    public Result<Void> favorite(@PathVariable Long id) { return Result.todo("收藏纹样"); }
//
//    @DeleteMapping("/{id}/favorite")
//    public Result<Void> cancelFavorite(@PathVariable Long id) { return Result.todo("取消收藏纹样"); }
//
//    @DeleteMapping("/{id}")
//    public Result<Void> delete(@PathVariable Long id) { return Result.todo("删除纹样"); }
//
//    @GetMapping("/{id}/download")
//    public Result<Void> download(@PathVariable Long id) { return Result.todo("下载纹样"); }


//================================上面接口错误,到时合并需要改=================================
    //todo
//纹样列表
    @GetMapping("/{patternId}")
    public Result<PatternMyVO> getPatternDetail(@PathVariable Long patternId) {
        Long userId = LoginUserHolder.getUserId();
        PatternMyVO vo =patternService.getPatternDetail(patternId,userId);
        return Result.success(vo);
    }
//保存纹样
    @PostMapping("/{patternId}/save")
    public Result<Void> savePattern(@PathVariable Long patternId) {
        Long userId = LoginUserHolder.getRequiredUserId();
        patternService.savePattern(patternId,userId);
        return Result.success();
    }
//收藏纹样
    @PostMapping("/{patternId}/favorite")
    public Result<Void> favoritePattern(@PathVariable Long patternId) {
        Long userId = LoginUserHolder.getRequiredUserId();
        patternService.favoritePattern(patternId,userId);
        return Result.success();
    }
//取消收藏纹样
    @DeleteMapping("/{patternId}/favorite")
    public Result<Void> unfavoritePattern(@PathVariable Long patternId) {
        Long userId = LoginUserHolder.getRequiredUserId();
        patternService.unfavoritePattern(patternId,userId);
        return Result.success();
    }
//删除我的纹样
    @DeleteMapping("/{patternId}")
    public Result<Void> patternDeleted(@PathVariable Long patternId) {
        Long userId = LoginUserHolder.getRequiredUserId();
        patternService.patternDeleted(patternId,userId);
        return Result.success();
    }

    /**
     * 7.7 获取纹样下载地址
     * GET /api/patterns/{patternId}/download
     */
    @GetMapping("/{patternId}/download")
    public Result<Map<String, Object>> getPatternDownloadUrl(@PathVariable Long patternId) {
        Long userId = LoginUserHolder.getRequiredUserId();
        Map<String, Object> data = patternService.getPatternDownloadUrl(patternId, userId);
        return Result.success(data);
    }
}

