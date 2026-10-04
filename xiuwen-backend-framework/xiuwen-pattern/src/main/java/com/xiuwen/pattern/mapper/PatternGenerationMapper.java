package com.xiuwen.pattern.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiuwen.pattern.entity.PatternGeneration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * pattern_generation 表 Mapper。
 */
@Mapper
public interface PatternGenerationMapper extends BaseMapper<PatternGeneration> {

    /**
     * 商家端 AI 纹样生成记录分页列表（关联 user 表获取昵称）
     */
    IPage<PatternGenerationWithUser> selectAdminGenerationPage(IPage<Object> page,
                                                               @Param("userId") Long userId,
                                                               @Param("keyword") String keyword,
                                                               @Param("style") String style,
                                                               @Param("status") String status,
                                                               @Param("startTime") String startTime,
                                                               @Param("endTime") String endTime);

    /**
     * 根据生成记录 ID 统计关联的纹样数量
     */
    Integer countPatternsByGenerationId(@Param("generationId") Long generationId);

    /** VO 类：生成记录带用户信息 */
    class PatternGenerationWithUser {
        private Long id;
        private Long userId;
        private String userNickname;
        private String keyword;
        private String style;
        private String elements;
        private String colorTheme;
        private String usageScene;
        private String description;
        private String referenceImageUrl;
        private String promptText;
        private Integer generateCount;
        private String status;
        private String errorMessage;
        private String createdAt;
        private Integer patternCount;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getUserNickname() { return userNickname; }
        public void setUserNickname(String userNickname) { this.userNickname = userNickname; }

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }

        public String getStyle() { return style; }
        public void setStyle(String style) { this.style = style; }

        public String getElements() { return elements; }
        public void setElements(String elements) { this.elements = elements; }

        public String getColorTheme() { return colorTheme; }
        public void setColorTheme(String colorTheme) { this.colorTheme = colorTheme; }

        public String getUsageScene() { return usageScene; }
        public void setUsageScene(String usageScene) { this.usageScene = usageScene; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getReferenceImageUrl() { return referenceImageUrl; }
        public void setReferenceImageUrl(String referenceImageUrl) { this.referenceImageUrl = referenceImageUrl; }

        public String getPromptText() { return promptText; }
        public void setPromptText(String promptText) { this.promptText = promptText; }

        public Integer getGenerateCount() { return generateCount; }
        public void setGenerateCount(Integer generateCount) { this.generateCount = generateCount; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public Integer getPatternCount() { return patternCount; }
        public void setPatternCount(Integer patternCount) { this.patternCount = patternCount; }
    }
}
