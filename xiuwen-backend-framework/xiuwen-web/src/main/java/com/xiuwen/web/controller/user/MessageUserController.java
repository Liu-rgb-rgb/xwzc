package com.xiuwen.web.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.framework.security.LoginUserHolder;
import com.xiuwen.system.entity.MessageNotice;
import com.xiuwen.system.service.MessageNoticeService;
import org.springframework.web.bind.annotation.*;

/**
 * 用户消息接口。
 */
@RestController
@RequestMapping("/api/messages")
public class MessageUserController {

    private final MessageNoticeService messageNoticeService;

    public MessageUserController(MessageNoticeService messageNoticeService) {
        this.messageNoticeService = messageNoticeService;
    }

    /** [4.1] 消息列表 */
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
        query.orderByDesc(MessageNotice::getIsRead);
        query.orderByDesc(MessageNotice::getCreatedAt);

        Page<MessageNotice> pageParam = new Page<>(page, pageSize);
        Page<MessageNotice> result = messageNoticeService.page(pageParam, query);
        return Result.success(PageResult.of(
                result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /** [4.2] 未读消息数量 */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = LoginUserHolder.getRequiredUserId();
        long count = messageNoticeService.count(new LambdaQueryWrapper<MessageNotice>()
                .and(w -> w.eq(MessageNotice::getUserId, userId).or().isNull(MessageNotice::getUserId))
                .eq(MessageNotice::getIsRead, 0));
        return Result.success(count);
    }

    /** [4.3] 标记单条消息已读 */
    @PutMapping("/{messageId}/read")
    public Result<Void> read(@PathVariable Long messageId) {
        MessageNotice msg = messageNoticeService.getById(messageId);
        if (msg == null) {
            throw new BusinessException("消息不存在");
        }
        Long userId = LoginUserHolder.getRequiredUserId();
        if (msg.getUserId() != null && !msg.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此消息");
        }
        if (msg.getIsRead() == null || msg.getIsRead() == 0) {
            MessageNotice update = new MessageNotice();
            update.setId(messageId);
            update.setIsRead(1);
            messageNoticeService.updateById(update);
        }
        return Result.success();
    }

    /** [4.4] 全部标记已读 */
    @PutMapping("/read-all")
    public Result<Void> readAll() {
        Long userId = LoginUserHolder.getRequiredUserId();
        messageNoticeService.update(new LambdaUpdateWrapper<MessageNotice>()
                .set(MessageNotice::getIsRead, 1)
                .eq(MessageNotice::getUserId, userId)
                .eq(MessageNotice::getIsRead, 0));
        return Result.success();
    }

    /** [4.5] 删除消息 */
    @DeleteMapping("/{messageId}")
    public Result<Void> deleteMessage(@PathVariable Long messageId) {
        MessageNotice msg = messageNoticeService.getById(messageId);
        if (msg == null) {
            throw new BusinessException("消息不存在");
        }
        Long userId = LoginUserHolder.getRequiredUserId();
        if (msg.getUserId() != null && !msg.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此消息");
        }
        messageNoticeService.removeById(messageId);
        return Result.success();
    }
}
