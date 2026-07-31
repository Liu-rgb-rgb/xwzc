package com.xiuwen.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * custom_design 表实体。
 */
@Data
@TableName("custom_design")
public class CustomDesign implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    private Integer deleted;
    private Long userId;
    private Long productId;
    private Long patternId;
    private String previewImageUrl;
    private String designConfig;
    private String remark;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
