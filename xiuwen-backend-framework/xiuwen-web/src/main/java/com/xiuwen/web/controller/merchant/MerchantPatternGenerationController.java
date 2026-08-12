package com.xiuwen.web.controller.merchant;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.pattern.dto.GenerationAdminQueryDTO;
import com.xiuwen.pattern.mapper.PatternGenerationMapper;
import com.xiuwen.pattern.service.PatternGenerationService;
import com.xiuwen.pattern.vo.GenerationAdminVO;
import com.xiuwen.system.entity.User;
import com.xiuwen.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商家端 AI 纹样生成记录管理接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/pattern-generations")
@RequiredArgsConstructor
public class MerchantPatternGenerationController {

    private final PatternGenerationService patternGenerationService;
    private final UserService userService;

    /**
     * 7.1 AI 生成记录列表（分页，支持用户/关键词/风格/状态/时间筛选）
     */
    @GetMapping
    public Result<PageResult<GenerationAdminVO>> list(GenerationAdminQueryDTO query) {
        IPage<PatternGenerationMapper.PatternGenerationWithUser> page =
                patternGenerationService.adminGenerationList(query);

        List<PatternGenerationMapper.PatternGenerationWithUser> records = page.getRecords();

        // 批量获取用户昵称（补充 SQL 关联可能缺失的）
        Map<Long, String> nicknameMap = batchGetUserNicknames(records);

        List<GenerationAdminVO> voList = new ArrayList<>();
        for (PatternGenerationMapper.PatternGenerationWithUser record : records) {
            GenerationAdminVO vo = convertToVO(record);
            if (vo.getUserNickname() == null || vo.getUserNickname().isEmpty()) {
                vo.setUserNickname(nicknameMap.get(record.getUserId()));
            }
            voList.add(vo);
        }

        return Result.success(PageResult.of(
                page.getTotal(),
                page.getCurrent(),
                page.getSize(),
                voList
        ));
    }

    // ==================== 私有辅助方法 ====================

    private GenerationAdminVO convertToVO(PatternGenerationMapper.PatternGenerationWithUser record) {
        GenerationAdminVO vo = new GenerationAdminVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setUserNickname(record.getUserNickname());
        vo.setKeyword(record.getKeyword());
        vo.setStyle(record.getStyle());
        vo.setElements(parseElements(record.getElements()));
        vo.setColorTheme(record.getColorTheme());
        vo.setUsageScene(record.getUsageScene());
        vo.setDescription(record.getDescription());
        vo.setReferenceImageUrl(record.getReferenceImageUrl());
        vo.setPromptText(record.getPromptText());
        vo.setGenerateCount(record.getGenerateCount());
        vo.setStatus(record.getStatus());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setPatternCount(0);
        return vo;
    }

    private List<String> parseElements(String elementsJson) {
        if (elementsJson == null || elementsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(elementsJson, String.class);
        } catch (Exception e) {
            log.warn("解析纹样元素 JSON 失败: {}", elementsJson, e);
            return new ArrayList<>();
        }
    }

    private Map<Long, String> batchGetUserNicknames(
            List<PatternGenerationMapper.PatternGenerationWithUser> records) {
        Set<Long> userIds = records.stream()
                .map(PatternGenerationMapper.PatternGenerationWithUser::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : ""));
    }
}