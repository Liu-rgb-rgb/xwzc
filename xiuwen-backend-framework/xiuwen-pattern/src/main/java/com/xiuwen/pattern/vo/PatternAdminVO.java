package com.xiuwen.pattern.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 商家端纹样列表/详情响应对象
 */
@Data
public class PatternAdminVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long generationId;
    private Long userId;
    private String title;
    private String imageUrl;
    private String thumbnailUrl;
    private String keyword;
    private String style;
    private List<String> elements;
    private String colorTheme;
    private String usageScene;
    private String description;
    private Integer isSaved;
    private Integer isFavorite;
    private Integer isRecommend;
    private Integer viewCount;
    private Integer likeCount;
    private Integer useCount;
    private String status;
    private String createdAt;
    private String updatedAt;
    private String userNickname;

    public static PatternAdminVO from(PatternMyVO vo) {
        if (vo == null) return null;
        PatternAdminVO out = new PatternAdminVO();
        out.setId(vo.getId());
        out.setGenerationId(vo.getGenerationId());
        out.setUserId(vo.getUserId());
        out.setTitle(vo.getTitle());
        out.setImageUrl(vo.getImageUrl());
        out.setThumbnailUrl(vo.getThumbnailUrl());
        out.setKeyword(vo.getKeyword());
        out.setStyle(vo.getStyle());
        out.setElements(vo.getElements());
        out.setColorTheme(vo.getColorTheme());
        out.setUsageScene(vo.getUsageScene());
        out.setDescription(vo.getDescription());
        out.setIsSaved(vo.getIsSaved());
        out.setIsFavorite(vo.getIsFavorite());
        out.setIsRecommend(vo.getIsRecommend());
        out.setViewCount(vo.getViewCount());
        out.setLikeCount(vo.getLikeCount());
        out.setUseCount(vo.getUseCount());
        out.setStatus(vo.getStatus());
        out.setCreatedAt(vo.getCreatedAt());
        out.setUpdatedAt(vo.getUpdatedAt());
        out.setUserNickname(vo.getUserNickname());
        return out;
    }
}
