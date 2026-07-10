package com.xiuwen.pattern.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * prompt_template 表实体。
 */
@Data
@TableName("prompt_template")
public class PromptTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    private Integer deleted;
    private String name;
    private String style;
    private String usageScene;
    private String colorTheme;
    private String templateText;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
