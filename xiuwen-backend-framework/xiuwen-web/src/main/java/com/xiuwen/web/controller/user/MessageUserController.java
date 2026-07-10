package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 用户消息接口。
 */
@RestController
@RequestMapping("/api/messages")
public class MessageUserController {


    @GetMapping
    public Result<Void> list() { return Result.todo("消息列表"); }

    @GetMapping("/unread-count")
    public Result<Void> unreadCount() { return Result.todo("未读消息数量"); }

    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) { return Result.todo("标记消息已读"); }

}
