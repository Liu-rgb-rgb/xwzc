package com.xiuwen.pattern.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.pattern.dto.GenerationAdminQueryDTO;
import com.xiuwen.pattern.entity.Pattern;
import com.xiuwen.pattern.entity.PatternGeneration;
import com.xiuwen.pattern.mapper.PatternGenerationMapper;
import com.xiuwen.pattern.service.PatternGenerationService;
import com.xiuwen.pattern.service.PatternService;
import com.xiuwen.pattern.vo.GenerationStatusVO;
import com.xiuwen.pattern.vo.PatternItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * pattern_generation 表服务实现。
 */
@Service
@RequiredArgsConstructor
public class PatternGenerationServiceImpl extends ServiceImpl<PatternGenerationMapper, PatternGeneration> implements PatternGenerationService {

    /** PROCESSING 状态超过该时长未更新, 视为任务中断(如后端重启), 自愈为 FAILED */
    private static final long STALE_PROCESSING_MINUTES = 10;

    private final PatternGenerationMapper patternGenerationMapper;
    private final PatternService patternService;

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

    @Override
    public GenerationStatusVO getGenerationStatus(Long generationId, Long userId) {
        PatternGeneration generation = patternGenerationMapper.selectById(generationId);
        if (generation == null) {
            throw new BusinessException("生成记录不存在");
        }
        if (!userId.equals(generation.getUserId())) {
            throw new BusinessException("无权查看他人的生成记录");
        }
        generation = healStaleProcessing(generation);

        List<Pattern> patterns = patternService.list(
                new LambdaQueryWrapper<Pattern>()
                        .eq(Pattern::getGenerationId, generationId)
                        .orderByAsc(Pattern::getId)
        );

        GenerationStatusVO vo = new GenerationStatusVO();
        vo.setId(generation.getId());
        vo.setStatus(generation.getStatus());
        vo.setProgress(generation.getProgress() == null ? 0 : generation.getProgress());
        vo.setTotalCount(generation.getGenerateCount() == null
                ? patterns.size()
                : generation.getGenerateCount());
        vo.setCompletedCount(patterns.size());
        vo.setErrorMessage(generation.getErrorMessage());
        vo.setCreatedAt(generation.getCreatedAt());
        vo.setUpdatedAt(generation.getUpdatedAt());
        vo.setPatterns(toPatternItems(patterns));
        return vo;
    }

    /**
     * 长时间停留在 PROCESSING 的记录(后端重启等导致任务丢失)标记为 FAILED, 避免前端无限轮询。
     */
    private PatternGeneration healStaleProcessing(PatternGeneration generation) {
        if (!"PROCESSING".equals(generation.getStatus())
                || generation.getUpdatedAt() == null
                || !generation.getUpdatedAt().isBefore(LocalDateTime.now().minusMinutes(STALE_PROCESSING_MINUTES))) {
            return generation;
        }
        PatternGeneration update = new PatternGeneration();
        update.setId(generation.getId());
        update.setStatus("FAILED");
        update.setErrorMessage("生成任务已中断，请重新生成");
        update.setUpdatedAt(LocalDateTime.now());
        patternGenerationMapper.updateById(update);
        generation.setStatus(update.getStatus());
        generation.setErrorMessage(update.getErrorMessage());
        return generation;
    }

    private List<PatternItemVO> toPatternItems(List<Pattern> patterns) {
        List<PatternItemVO> items = new ArrayList<>();
        for (Pattern pattern : patterns) {
            PatternItemVO item = new PatternItemVO();
            item.setId(pattern.getId());
            item.setTitle(pattern.getTitle());
            item.setImageUrl(pattern.getImageUrl());
            item.setThumbnailUrl(pattern.getThumbnailUrl());
            item.setStyle(pattern.getStyle());
            item.setIsFavorite(false);
            item.setCreatedAt(pattern.getCreatedAt());
            items.add(item);
        }
        return items;
    }
}
