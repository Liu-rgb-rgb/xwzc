package com.xiuwen.pattern.vo;

import lombok.Data;

/**
 * 提交 AI 生成任务后的响应。
 *
 * 生图为异步任务: 提交接口立即返回 generationId,
 * 前端凭此 ID 轮询生成状态。
 */
@Data
public class GenerationSubmitVO {
    private Long generationId;
    private String status;
}
