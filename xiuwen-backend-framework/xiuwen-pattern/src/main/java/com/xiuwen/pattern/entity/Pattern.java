package com.xiuwen.pattern.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * pattern 表实体。
 */
@Data
@TableName("pattern")
public class Pattern implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    private Integer deleted;
    private Long generationId;
    private Long userId;
    private String title;
    private String imageUrl;
    private String thumbnailUrl;
    private String keyword;
    private String style;
    private String elements;
    private String usageScene;
    private String colorTheme;
    private String description;
    private Integer isFavorite;
    private Integer isRecommend;
    private Integer viewCount;
    private Integer likeCount;
    private Integer useCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
