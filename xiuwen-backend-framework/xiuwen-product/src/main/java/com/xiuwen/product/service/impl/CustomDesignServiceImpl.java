package com.xiuwen.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.product.entity.CustomDesign;
import com.xiuwen.product.entity.CustomDesignDetail;
import com.xiuwen.product.mapper.CustomDesignMapper;
import com.xiuwen.product.service.CustomDesignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * custom_design 表服务实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomDesignServiceImpl extends ServiceImpl<CustomDesignMapper, CustomDesign> implements CustomDesignService {

    @Override
    public CustomDesignDetail createDesignDetail(Long userId, Long productId, Long patternId, String designConfig, String remark) {
        CustomDesign design = new CustomDesign();
        design.setUserId(userId);
        design.setProductId(productId);
        design.setPatternId(patternId);
        design.setDesignConfig(designConfig);
        design.setRemark(remark);
        design.setStatus("NORMAL");

        // 生成预览图URL（实际项目中应调用图片合成服务）
        design.setPreviewImageUrl("https://cdn.example.com/custom/preview-" + System.currentTimeMillis() + ".png");

        save(design);
        return baseMapper.selectDesignWithDetails(design.getId());
    }

    @Override
    public IPage<CustomDesign> pageMyDesign(Long userId, int page, int pageSize, String status) {
        LambdaQueryWrapper<CustomDesign> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomDesign::getUserId, userId);

        if (StringUtils.hasText(status)) {
            wrapper.eq(CustomDesign::getStatus, status);
        } else {
            wrapper.eq(CustomDesign::getStatus, "NORMAL");
        }

        wrapper.orderByDesc(CustomDesign::getCreatedAt);

        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public CustomDesignDetail getDesignDetail(Long designId) {
        return baseMapper.selectDesignWithDetails(designId);
    }

    @Override
    public void deletedDesign(Long userId, Long customDesignId) {
        LambdaQueryWrapper<CustomDesign> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomDesign::getUserId, userId);
        wrapper.eq(CustomDesign::getId, customDesignId);
        boolean remove = this.remove(wrapper);
        if (!remove) {
            throw new BusinessException("定制方案不存在或无权操作");
        }
    }

    @Override
    public IPage<CustomDesignDetail> pageAdminDesigns(int page, int pageSize, Long userId, Long productId, String status) {
        return baseMapper.selectAdminDesignList(new Page<>(page, pageSize), userId, productId, status);
    }

    @Override
    public Map<String, Object> getDownloadUrls(Long customDesignId) {
        CustomDesignDetail detail = baseMapper.selectDesignWithDetails(customDesignId);
        if (detail == null) {
            throw new BusinessException("定制方案不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("previewDownloadUrl", detail.getPreviewImageUrl());
        result.put("patternDownloadUrl", detail.getPatternImageUrl());
        result.put("expiresIn", 600);
        return result;
    }
}
