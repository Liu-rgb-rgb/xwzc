package com.xiuwen.pattern.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 商家端 AI 纹样生成记录列表响应对象。
 */
@Data
public class GenerationAdminVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 生成记录 ID */
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** 用户昵称 */
    private String userNickname;

    /** 生成关键词 */
    private String keyword;

    /** 纹样风格 */
    private String style;

    /** 纹样元素数组（JSON 字符串） */
    private List<String> elements;

    /** 颜色主题 */
    private String colorTheme;

    /** 应用场景 */
    private String usageScene;

    /** 用户补充描述 */
    private String description;

    /** 参考图 URL */
    private String referenceImageUrl;

    /** 最终提示词 */
    private String promptText;

    /** 本次生成图片数量 */
    private Integer generateCount;

    /** 生成状态：PENDING / SUCCESS / FAILED */
    private String status;

    /** 生成失败原因 */
    private String errorMessage;

    /** 创建时间 */
    private String createdAt;

    /** 关联的纹样数量 */
    private Integer patternCount;
}
