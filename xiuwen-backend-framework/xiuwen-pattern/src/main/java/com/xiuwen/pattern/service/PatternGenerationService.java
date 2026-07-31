package com.xiuwen.pattern.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiuwen.pattern.dto.GenerationAdminQueryDTO;
import com.xiuwen.pattern.entity.PatternGeneration;
import com.xiuwen.pattern.mapper.PatternGenerationMapper;

/**
 * pattern_generation 表服务接口。
 */
public interface PatternGenerationService extends IService<PatternGeneration> {

    /**
     * 商家端 AI 纹样生成记录分页列表
     */
    IPage<PatternGenerationMapper.PatternGenerationWithUser> adminGenerationList(GenerationAdminQueryDTO query);
}
