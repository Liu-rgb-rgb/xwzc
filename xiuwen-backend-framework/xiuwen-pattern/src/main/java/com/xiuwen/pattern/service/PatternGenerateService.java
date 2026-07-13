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
ai纹样生成
*/
import com.xiuwen.pattern.dto.RegeneratePatternRequest;
import com.xiuwen.pattern.vo.GeneratePatternResponse;
import com.xiuwen.pattern.dto.GeneratePatternRequest;
public interface PatternGenerateService {
    /*当前登录用户ID
    * 用户提交的生成参数
    * 生成结果*/
 GeneratePatternResponse generate(Long userId, GeneratePatternRequest request);
 GeneratePatternResponse regenerate(Long userId,Long generationId);
}
