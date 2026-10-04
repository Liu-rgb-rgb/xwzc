package com.xiuwen.pattern.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiuwen.pattern.dto.GenerationAdminQueryDTO;
import com.xiuwen.pattern.entity.PatternGeneration;
import com.xiuwen.pattern.mapper.PatternGenerationMapper;
import com.xiuwen.pattern.vo.GenerationStatusVO;

/**
 * pattern_generation 表服务接口。
 */
public interface PatternGenerationService extends IService<PatternGeneration> {

    /**
     * 商家端 AI 纹样生成记录分页列表
     */
    IPage<PatternGenerationMapper.PatternGenerationWithUser> adminGenerationList(GenerationAdminQueryDTO query);

    /**
     * 用户端查询自己的生成任务状态(前端轮询用)。
     *
     * @param generationId 生成记录ID
     * @param userId       当前登录用户ID(校验归属)
     */
    GenerationStatusVO getGenerationStatus(Long generationId, Long userId);
}
