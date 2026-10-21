package com.xiuwen.pattern.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 纹样实体类，对应数据库表 pattern
 */
@Data
@TableName("pattern")
public class Pattern {

    /**
     * 纹样ID，自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * AI 生成记录ID，对应 pattern_generation.id
     */
    private Long generationId;

    /**
     * 所属用户ID，对应 user.id
     */
    private Long userId;

    /**
     * 纹样标题
     */
    private String title;

    /**
     * 纹样原图 URL
     */
    private String imageUrl;

    /**
     * 纹样缩略图 URL
     */
    private String thumbnailUrl;

    /**
     * 生成关键词快照
     */
    private String keyword;

    /**
     * 纹样风格：classic / new_chinese / embroidery 等
     */
    private String style;

    /**
     * 纹样元素数组，JSON 存储：["牡丹","凤凰","祥云"]
     */
    private String elements;

    /**
     * 颜色主题，如 "富贵华彩"
     */
    private String colorTheme;

    /**
     * 应用场景：product / clothing / home 等
     */
    private String usageScene;

    /**
     * 纹样说明
     */
    private String description;

    /**
     * 是否已保存到我的纹样：1 是，0 否
     */
    private Integer isSaved;

    /**
     * 是否收藏：1 是，0 否
     */
    private Integer isFavorite;
    /**
     * 是否删除：1 是，0 否
     */
    private Integer deleted;
    /**
     * 是否首页推荐：1 是，0 否
     */
    private Integer isRecommend;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 点赞或收藏热度数量
     */
    private Integer likeCount;

    /**
     * 被应用到商品定制的次数
     */
    private Integer useCount;

    /**
     * 状态：NORMAL 正常 / HIDDEN 隐藏 / DELETED 删除
     */
    private String status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
