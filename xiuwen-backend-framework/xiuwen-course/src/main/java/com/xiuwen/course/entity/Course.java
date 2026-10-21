package com.xiuwen.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * course 表实体。
 */
@Data
@TableName("course")
public class Course implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    private Integer deleted;
    private Long categoryId;
    private String title;
    private String subtitle;
    private String coverImage;
    private BigDecimal price;
    private String teacherName;
    private String duration;
    private String difficulty;
    private String description;
    private String content;
    private String videoUrl;
    private Integer viewCount;
    private Integer studyCount;
    private Integer isRecommend;
    private Integer sort;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String categoryName;
}
