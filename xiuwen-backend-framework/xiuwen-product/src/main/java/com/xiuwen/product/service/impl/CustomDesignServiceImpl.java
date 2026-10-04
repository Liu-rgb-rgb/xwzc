package com.xiuwen.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.product.entity.CustomDesign;
import com.xiuwen.product.entity.CustomDesignDetail;
import com.xiuwen.product.entity.Product;
import com.xiuwen.product.mapper.CustomDesignMapper;
import com.xiuwen.product.service.CustomDesignService;
import com.xiuwen.product.service.ProductService;
import com.xiuwen.framework.service.OssFileService;
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

    private final OssFileService ossFileService;
    private final ProductService productService;

    @Override
    public CustomDesignDetail createDesignDetail(Long userId, Long productId, Long patternId, String designConfig, String previewImageUrl, String remark) {
        Product product = productService.getById(productId);
        if (product == null || !"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException("商品不存在或已下架");
        }
        if (!Integer.valueOf(1).equals(product.getIsCustomizable())) {
            throw new BusinessException("该商品不支持定制");
        }
        if (product.getStock() == null || product.getStock() <= 0) {
            throw new BusinessException("商品已售罄，暂不可定制");
        }
        CustomDesign design = new CustomDesign();
        design.setUserId(userId);
        design.setProductId(productId);
        design.setPatternId(patternId);
        design.setDesignConfig(designConfig);
        design.setRemark(remark);
        design.setStatus("NORMAL");

        if (StringUtils.hasText(previewImageUrl)) {
            design.setPreviewImageUrl(previewImageUrl.trim());
        } else {
            design.setPreviewImageUrl(
                    ossFileService.getOssDomain() + "custom/preview-" + System.currentTimeMillis() + ".png"
            );
        }

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
