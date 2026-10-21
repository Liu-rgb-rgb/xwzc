package com.xiuwen.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * message_notice 表实体。
 */
@Data
@TableName("message_notice")
public class MessageNotice implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    private Integer deleted;
    private Long userId;
    private String title;
    private String content;
    private String noticeType;
    private Integer isRead;
    private String relatedType;
    private Long relatedId;
    private LocalDateTime createdAt;
}
