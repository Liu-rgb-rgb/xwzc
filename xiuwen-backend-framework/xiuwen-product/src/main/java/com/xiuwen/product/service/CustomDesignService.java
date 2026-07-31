package com.xiuwen.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiuwen.product.entity.CustomDesign;
import com.xiuwen.product.entity.CustomDesignDetail;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * custom_design 表服务接口。
 */
public interface CustomDesignService extends IService<CustomDesign> {
    CustomDesign createDesignDetail(Long userId, @NotNull(message = "商品id不能为空") Long productId, @NotNull(message = "纹样id不能为空") Long patternId, String designConfig, String remark);

    IPage<CustomDesign> pageMyDesign(Long userId, int page, int pageSize, String status);

    CustomDesignDetail getDesignDetail(Long id);

    void deletedDesign(Long userId, Long customDesignId);

    IPage<CustomDesignDetail> pageAdminDesigns(int page, int pageSize, Long userId, Long productId, String status);

    Map<String, Object> getDownloadUrls(Long customDesignId);
}
