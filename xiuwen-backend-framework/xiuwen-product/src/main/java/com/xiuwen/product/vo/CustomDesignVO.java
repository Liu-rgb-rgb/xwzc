package com.xiuwen.product.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.xiuwen.product.entity.CustomDesign;
import com.xiuwen.product.entity.CustomDesignDetail;
import lombok.Data;

/**
 * 定制方案 VO
 */
@Data
public class CustomDesignVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
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
    private String userNickname;

    public static CustomDesignVO fromDetail(CustomDesign entity) {
        if (entity == null) { return null; }
        CustomDesignVO vo = new CustomDesignVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setProductId(entity.getProductId());
        vo.setPatternId(entity.getPatternId());
        vo.setPreviewImageUrl(entity.getPreviewImageUrl());
        vo.setDesignConfig(entity.getDesignConfig());
        vo.setRemark(entity.getRemark());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());

        if (entity instanceof CustomDesignDetail detail) {
            vo.setProductName(detail.getProductName());
            vo.setProductCoverImage(detail.getProductCoverImage());
            vo.setPatternTitle(detail.getPatternTitle());
            vo.setPatternImageUrl(detail.getPatternImageUrl());
            vo.setUserNickname(detail.getUserNickname());
        }
        return vo;
    }
}
