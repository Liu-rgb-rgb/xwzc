package com.xiuwen.pattern.vo;

import lombok.Data;

import java.util.List;

/**
 * 我的纹样列表 / 纹样详情 响应对象
 * 对应接口：GET /api/patterns/my、GET /api/patterns/{patternId}
 */
@Data
public class PatternMyVO {

    /**
     * 纹样ID，自增主键
     */
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
     * 纹样元素数组快照，如 ["牡丹", "凤凰", "祥云"]
     */
    private List<String> elements;

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
     * 创建时间，格式 yyyy-MM-dd HH:mm:ss
     */
    private String createdAt;

    /**
     * 更新时间，格式 yyyy-MM-dd HH:mm:ss
     */
    private String updatedAt;

    /**
     * 所属用户昵称（仅详情接口返回）
     */
    private String userNickname;
}