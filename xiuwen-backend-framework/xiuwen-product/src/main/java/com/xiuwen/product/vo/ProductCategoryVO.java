package com.xiuwen.product.vo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ClassName: ProductCategoryVO
 * Package: com.xiuwen.product.vo
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/19 15:23
 * @Version 1.0
 */
@Data
public class ProductCategoryVO {
    private Long id;
    private Long parentId;
    private String name;
    private String icon;
    private Integer sort;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
