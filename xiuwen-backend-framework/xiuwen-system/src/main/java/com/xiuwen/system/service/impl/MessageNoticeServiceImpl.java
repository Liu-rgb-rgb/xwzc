package com.xiuwen.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.system.entity.MessageNotice;
import com.xiuwen.system.mapper.MessageNoticeMapper;
import com.xiuwen.system.service.MessageNoticeService;
import org.springframework.stereotype.Service;

/**
 * message_notice 表服务实现。
 */
@Service
public class MessageNoticeServiceImpl extends ServiceImpl<MessageNoticeMapper, MessageNotice> implements MessageNoticeService {
}
