package com.xiuwen.pattern.service;

/**
 * ClassName: PatternGenerate
 * Package: com.xiuwen.pattern.service
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/11 11:18
 * @Version 1.0
 */
/*
ai纹样生成(异步): 提交后立即返回 generationId, 后台线程池执行生图, 前端轮询状态
*/
import com.xiuwen.pattern.dto.RegeneratePatternRequest;
import com.xiuwen.pattern.vo.GenerationSubmitVO;
import com.xiuwen.pattern.dto.GeneratePatternRequest;
public interface PatternGenerateService {
    /*当前登录用户ID
    * 用户提交的生成参数
    * 提交结果(含 generationId, 供轮询)*/
    GenerationSubmitVO generate(Long userId, GeneratePatternRequest request);
    GenerationSubmitVO regenerate(
            Long userId,
            RegeneratePatternRequest request
    );
}
