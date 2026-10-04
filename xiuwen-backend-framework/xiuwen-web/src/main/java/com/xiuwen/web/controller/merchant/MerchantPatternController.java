package com.xiuwen.web.controller.merchant;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.pattern.dto.PatternAdminQueryDTO;
import com.xiuwen.pattern.entity.Pattern;
import com.xiuwen.pattern.entity.PatternAdminDetail;
import com.xiuwen.pattern.service.PatternService;
import com.xiuwen.pattern.vo.PatternAdminVO;
import com.xiuwen.system.entity.User;
import com.xiuwen.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商家端纹样管理接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/patterns")
@RequiredArgsConstructor
public class MerchantPatternController {

    private final PatternService patternService;
    private final UserService userService;

    /**
     * 6.1 纹样列表（分页，支持用户/关键词/风格/状态/推荐筛选）
     */
    @GetMapping
    public Result<PageResult<PatternAdminVO>> list(PatternAdminQueryDTO query) {
        IPage<PatternAdminDetail> page = patternService.adminPatternList(query);
        List<PatternAdminDetail> records = page.getRecords();

        // 批量获取用户昵称（虽然 SQL 已关联，但保险起见补充缺失的）
        Map<Long, String> nicknameMap = batchGetUserNicknames(records);

        List<PatternAdminVO> voList = new ArrayList<>();
        for (PatternAdminDetail detail : records) {
            PatternAdminVO vo = convertToVO(detail);
            if (vo.getUserNickname() == null || vo.getUserNickname().isEmpty()) {
                vo.setUserNickname(nicknameMap.get(detail.getUserId()));
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

    /**
     * 6.2 纹样详情
     */
    @GetMapping("/{patternId}")
    public Result<PatternAdminVO> detail(@PathVariable Long patternId) {
        Pattern pattern = patternService.getById(patternId);
        if (pattern == null) {
            return Result.fail("纹样不存在");
        }

        PatternAdminVO vo = new PatternAdminVO();
        vo.setId(pattern.getId());
        vo.setGenerationId(pattern.getGenerationId());
        vo.setUserId(pattern.getUserId());
        vo.setTitle(pattern.getTitle());
        vo.setImageUrl(pattern.getImageUrl());
        vo.setThumbnailUrl(pattern.getThumbnailUrl());
        vo.setKeyword(pattern.getKeyword());
        vo.setStyle(pattern.getStyle());
        vo.setElements(parseElements(pattern.getElements()));
        vo.setColorTheme(pattern.getColorTheme());
        vo.setUsageScene(pattern.getUsageScene());
        vo.setDescription(pattern.getDescription());
        vo.setIsSaved(pattern.getIsSaved());
        vo.setIsFavorite(pattern.getIsFavorite());
        vo.setIsRecommend(pattern.getIsRecommend());
        vo.setViewCount(pattern.getViewCount() != null ? pattern.getViewCount() : 0);
        vo.setLikeCount(pattern.getLikeCount() != null ? pattern.getLikeCount() : 0);
        vo.setUseCount(pattern.getUseCount() != null ? pattern.getUseCount() : 0);
        vo.setStatus(pattern.getStatus());
        vo.setCreatedAt(formatTime(pattern.getCreatedAt()));
        vo.setUpdatedAt(formatTime(pattern.getUpdatedAt()));

        // 获取用户昵称
        if (pattern.getUserId() != null) {
            User user = userService.getById(pattern.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname());
            }
        }

        return Result.success(vo);
    }

    /**
     * 6.3 设置或取消纹样推荐
     */
    @PutMapping("/{patternId}/recommend")
    public Result<Map<String, Object>> recommend(@PathVariable Long patternId,
                                                   @RequestBody SetRecommendRequest request) {
        patternService.setRecommend(patternId, request.getIsRecommend());

        Pattern pattern = patternService.getById(patternId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("patternId", patternId);
        data.put("isRecommend", pattern.getIsRecommend());
        data.put("updatedAt", formatTime(pattern.getUpdatedAt()));
        return Result.success(data);
    }

    /**
     * 6.4 隐藏或恢复纹样
     */
    @PutMapping("/{patternId}/status")
    public Result<Map<String, Object>> status(@PathVariable Long patternId,
                                                @RequestBody UpdateStatusRequest request) {
        patternService.updatePatternStatus(patternId, request.getStatus());

        Pattern pattern = patternService.getById(patternId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("patternId", patternId);
        data.put("status", pattern.getStatus());
        data.put("updatedAt", formatTime(pattern.getUpdatedAt()));
        return Result.success(data);
    }

    /**
     * 6.5 删除纹样（逻辑删除 + 状态改为 DELETED）
     */
    @DeleteMapping("/{patternId}")
    public Result<Void> delete(@PathVariable Long patternId) {
        Pattern pattern = patternService.getById(patternId);
        if (pattern == null) {
            return Result.fail("纹样不存在");
        }

        patternService.removeById(patternId);
        // 同时更新业务状态
        patternService.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Pattern>()
                        .eq(Pattern::getId, patternId)
                        .set(Pattern::getStatus, "DELETED"));
        return Result.success();
    }

    // ==================== 私有辅助方法 ====================

    private PatternAdminVO convertToVO(PatternAdminDetail detail) {
        PatternAdminVO vo = new PatternAdminVO();
        vo.setId(detail.getId());
        vo.setGenerationId(detail.getGenerationId());
        vo.setUserId(detail.getUserId());
        vo.setTitle(detail.getTitle());
        vo.setImageUrl(detail.getImageUrl());
        vo.setThumbnailUrl(detail.getThumbnailUrl());
        vo.setKeyword(detail.getKeyword());
        vo.setStyle(detail.getStyle());
        vo.setElements(parseElements(detail.getElements()));
        vo.setColorTheme(detail.getColorTheme());
        vo.setUsageScene(detail.getUsageScene());
        vo.setDescription(detail.getDescription());
        vo.setIsSaved(detail.getIsSaved());
        vo.setIsFavorite(detail.getIsFavorite());
        vo.setIsRecommend(detail.getIsRecommend());
        vo.setViewCount(detail.getViewCount());
        vo.setLikeCount(detail.getLikeCount());
        vo.setUseCount(detail.getUseCount());
        vo.setStatus(detail.getStatus());
        vo.setCreatedAt(detail.getCreatedAt());
        vo.setUpdatedAt(detail.getUpdatedAt());
        vo.setUserNickname(detail.getUserNickname());
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

    private String formatTime(java.time.LocalDateTime time) {
        if (time == null) return null;
        return time.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private Map<Long, String> batchGetUserNicknames(List<PatternAdminDetail> records) {
        Set<Long> userIds = records.stream()
                .map(PatternAdminDetail::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : ""));
    }

    // ==================== 请求体 ====================

    static class SetRecommendRequest {
        private Integer isRecommend;

        public Integer getIsRecommend() { return isRecommend; }
        public void setIsRecommend(Integer isRecommend) { this.isRecommend = isRecommend; }
    }

    static class UpdateStatusRequest {
        private String status;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
