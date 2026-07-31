package com.xiuwen.pattern.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.pattern.dto.GenerationAdminQueryDTO;
import com.xiuwen.pattern.entity.PatternGeneration;
import com.xiuwen.pattern.mapper.PatternGenerationMapper;
import com.xiuwen.pattern.service.PatternGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * pattern_generation 表服务实现。
 */
@Service
@RequiredArgsConstructor
public class PatternGenerationServiceImpl extends ServiceImpl<PatternGenerationMapper, PatternGeneration> implements PatternGenerationService {

    private final PatternGenerationMapper patternGenerationMapper;

    @Override
    public IPage<PatternGenerationMapper.PatternGenerationWithUser> adminGenerationList(GenerationAdminQueryDTO query) {
        Page<Object> page = new Page<>(query.getPage(), query.getPageSize());
        return patternGenerationMapper.selectAdminGenerationPage(page,
                query.getUserId(),
                query.getKeyword(),
                query.getStyle(),
                query.getStatus(),
                query.getStartTime(),
                query.getEndTime());
    }
}
