package com.xiuwen.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 发布消息通知请求。
 */
@Data
public class MessageSendRequest {
    @NotBlank(message = "消息标题不能为空")
    private String title;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    @NotBlank(message = "消息类型不能为空")
    private String noticeType;

    private String relatedType;
    private Long relatedId;
    private Long userId;
}
