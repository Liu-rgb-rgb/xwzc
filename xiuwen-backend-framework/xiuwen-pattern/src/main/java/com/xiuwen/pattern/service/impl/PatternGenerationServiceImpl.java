package com.xiuwen.pattern.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.pattern.entity.PatternGeneration;
import com.xiuwen.pattern.mapper.PatternGenerationMapper;
import com.xiuwen.pattern.service.PatternGenerationService;
import org.springframework.stereotype.Service;

/**
 * pattern_generation 表服务实现。
 */
@Service
public class PatternGenerationServiceImpl extends ServiceImpl<PatternGenerationMapper, PatternGeneration> implements PatternGenerationService {
}
