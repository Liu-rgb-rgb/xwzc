package com.xiuwen.web.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.framework.security.LoginUserHolder;
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

    /** 当前商家账号的消息列表 */
    @GetMapping
    public Result<PageResult<MessageNotice>> list(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) String noticeType,
                                                   @RequestParam(required = false) Integer isRead) {
        Long userId = LoginUserHolder.getRequiredUserId();
        LambdaQueryWrapper<MessageNotice> query = new LambdaQueryWrapper<>();
        query.and(w -> w.eq(MessageNotice::getUserId, userId).or().isNull(MessageNotice::getUserId));
        if (noticeType != null && !noticeType.isEmpty()) {
            query.eq(MessageNotice::getNoticeType, noticeType);
        }
        if (isRead != null) {
            query.eq(MessageNotice::getIsRead, isRead);
        }
        query.orderByAsc(MessageNotice::getIsRead)
                .orderByDesc(MessageNotice::getCreatedAt);

        Page<MessageNotice> result = messageNoticeService.page(new Page<>(page, pageSize), query);
        return Result.success(PageResult.of(
                result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @PutMapping("/{messageId}/read")
    public Result<Void> read(@PathVariable Long messageId) {
        Long userId = LoginUserHolder.getRequiredUserId();
        MessageNotice notice = messageNoticeService.getById(messageId);
        if (notice == null) {
            throw new BusinessException("消息不存在");
        }
        if (notice.getUserId() == null || !notice.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此消息");
        }
        if (!Integer.valueOf(1).equals(notice.getIsRead())) {
            MessageNotice update = new MessageNotice();
            update.setId(messageId);
            update.setIsRead(1);
            messageNoticeService.updateById(update);
        }
        return Result.success();
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
