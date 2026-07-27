package com.xiuwen.product.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.xiuwen.product.entity.CustomDesign;
import com.xiuwen.product.entity.CustomDesignDetail;
import lombok.Data;

/**
 * custom_design 表实体。
 */
@Data
@TableName("custom_design")
public class CustomDesignVO implements Serializable {
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


    private String productName;
    private String productCoverImage;
    private String patternTitle;
    private String patternImageUrl;
    private String uerNickname;
    public static CustomDesignVO fromDetail (CustomDesign entity){
        if(entity == null){ return null;}
        CustomDesignVO vo = new CustomDesignVO();
        vo.setId(entity.getId());
        vo.setDeleted(entity.getDeleted());
        vo.setUserId(entity.getUserId());
        vo.setProductId(entity.getProductId());
        vo.setPatternId(entity.getPatternId());
        vo.setPreviewImageUrl(entity.getPreviewImageUrl());
        vo.setDesignConfig(entity.getDesignConfig());
        vo.setRemark(entity.getRemark());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());

        if(entity instanceof CustomDesignDetail) {
            CustomDesignDetail detail  = (CustomDesignDetail) entity;
            vo.setProductName(detail.getProductName());
            vo.setProductCoverImage(detail.getProductCoverImage());
            vo.setPatternTitle(detail.getPatternTitle());
            vo.setPatternImageUrl(detail.getPatternImageUrl());
            vo.setUerNickname(detail.getUserNickname());
        }
        return vo;
    }



}
