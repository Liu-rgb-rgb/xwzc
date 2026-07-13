package com.xiuwen.pattern.vo;
import lombok.Data;
import java.util.List;
/**
 * ClassName: GeneratePatternResponse
 * Package: com.xiuwen.pattern.vo
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/11 10:57
 * @Version 1.0
 */
/*
* ai纹样生成结果*/
    @Data
public class GeneratePatternResponse {
    //生成本次任务的id
    private Long generationId;
    //生成的状态
    private String status;
    //生成的纹样
    private List<PatternItemVO> patterns;
}
