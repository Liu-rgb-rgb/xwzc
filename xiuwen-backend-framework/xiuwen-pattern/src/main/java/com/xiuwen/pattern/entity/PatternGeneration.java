package com.xiuwen.pattern.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * pattern_generation 表实体。
 */
@Data
@TableName("pattern_generation")
public class PatternGeneration implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    private Integer deleted;
    private Long userId;
    private String keyword;
    private String style;
    private String elements;
    private String usageScene;
    private String colorTheme;
    private String promptText;
    private Integer generateCount;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private String description;
    private String referenceImageUrl;
}
