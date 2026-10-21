package com.xiuwen.pattern.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 商家端纹样列表关联查询结果 VO（包含 user 表昵称字段）
 */
@Data
public class PatternAdminDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long generationId;
    private Long userId;
    private String title;
    private String imageUrl;
    private String thumbnailUrl;
    private String keyword;
    private String style;
    private String elements;
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
}
