package com.xiuwen.pattern.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.pattern.entity.Pattern;
import com.xiuwen.pattern.mapper.PatternMapper;
import com.xiuwen.pattern.service.PatternService;
import org.springframework.stereotype.Service;

/**
 * pattern 表服务实现。
 */
@Service
public class PatternServiceImpl extends ServiceImpl<PatternMapper, Pattern> implements PatternService {
}
