package com.xiuwen.pattern.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 生成任务状态查询结果(前端轮询用)。
 */
@Data
public class GenerationStatusVO {
    private Long id;
    /** PROCESSING / SUCCESS / FAILED */
    private String status;
    /** 进度 0-100 */
    private Integer progress;
    /** 已完成的图片数 */
    private Integer completedCount;
    /** 本次要生成的图片总数 */
    private Integer totalCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 已生成的纹样(随进度逐步增加) */
    private List<PatternItemVO> patterns = new ArrayList<>();
}
