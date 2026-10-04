package com.xiuwen.web.controller.user;


import com.xiuwen.common.core.domain.Result;
import com.xiuwen.framework.security.LoginUserHolder;
import com.xiuwen.pattern.service.PatternGenerationService;
import com.xiuwen.pattern.vo.GenerationStatusVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 AI 生成记录接口(我的生成记录)。
 */
@RestController
@RequestMapping("/api/pattern-generations")
public class PatternGenerationUserController {

    private final PatternGenerationService patternGenerationService;

    public PatternGenerationUserController(PatternGenerationService patternGenerationService) {
        this.patternGenerationService = patternGenerationService;
    }

    /**
     * 查询生成任务状态(前端轮询)。
     * 返回状态、进度、已完成图片数及已生成的纹样列表;
     * 任务只允许本人查询。
     */
    @GetMapping("/{id}")
    public Result<GenerationStatusVO> status(@PathVariable Long id) {
        Long userId = LoginUserHolder.getRequiredUserId();
        return Result.success(patternGenerationService.getGenerationStatus(id, userId));
    }
}
