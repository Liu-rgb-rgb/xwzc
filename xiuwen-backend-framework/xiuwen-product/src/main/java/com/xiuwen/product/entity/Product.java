package com.xiuwen.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * product 表实体。
 */
@Data
@TableName("product")
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    private Integer deleted;
    private Long categoryId;
    private String name;
    private String subtitle;
    private BigDecimal price;
    private Integer stock;
    private String coverImage;
    private String mockupImage;
    private String description;
    private Integer isCustomizable;
    private Integer isRecommend;
    private Integer salesCount;
    private Integer sort;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
