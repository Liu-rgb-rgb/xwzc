package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;
import com.xiuwen.system.dto.MessageSendRequest;
import com.xiuwen.system.entity.MessageNotice;
import com.xiuwen.system.service.MessageNoticeService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 商家端消息管理接口。
 */
@RestController
@RequestMapping("/api/admin/messages")
public class MerchantMessageController {

    private final MessageNoticeService messageNoticeService;

    public MerchantMessageController(MessageNoticeService messageNoticeService) {
        this.messageNoticeService = messageNoticeService;
    }

    /** [14.1] 发布消息通知 */
    @PostMapping
    public Result<MessageNotice> sendMessage(@Valid @RequestBody MessageSendRequest request) {
        MessageNotice notice = new MessageNotice();
        notice.setUserId(request.getUserId());
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setNoticeType(request.getNoticeType());
        notice.setRelatedType(request.getRelatedType());
        notice.setRelatedId(request.getRelatedId());
        notice.setIsRead(0);
        messageNoticeService.save(notice);
        return Result.success(notice);
    }
}
