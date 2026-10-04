package com.xiuwen.web.controller.merchant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.product.entity.CustomDesignDetail;
import com.xiuwen.product.service.CustomDesignService;
import com.xiuwen.product.vo.CustomDesignVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商家端定制管理接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/custom-designs")
@RequiredArgsConstructor
public class MerchantCustomDesignController {

    private final CustomDesignService customDesignService;

    /**
     * 5.1 定制方案列表
     */
    @GetMapping
    public Result<PageResult<CustomDesignVO>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {

        IPage<CustomDesignDetail> pageResult = customDesignService.pageAdminDesigns(
                page, pageSize, userId, productId, status);

        List<CustomDesignVO> voList = pageResult.getRecords().stream()
                .map(CustomDesignVO::fromDetail)
                .collect(Collectors.toList());

        return Result.success(PageResult.of(
                pageResult.getTotal(),
                pageResult.getCurrent(),
                pageResult.getSize(),
                voList));
    }

    /**
     * 5.2 定制方案详情
     */
    @GetMapping("/{customDesignId}")
    public Result<CustomDesignVO> detail(@PathVariable Long customDesignId) {
        CustomDesignDetail detail = customDesignService.getDesignDetail(customDesignId);
        return Result.success(CustomDesignVO.fromDetail(detail));
    }

    /**
     * 5.3 下载定制预览图或原纹样
     */
    @GetMapping("/{customDesignId}/download")
    public Result<Map<String, Object>> download(@PathVariable Long customDesignId) {
        Map<String, Object> urls = customDesignService.getDownloadUrls(customDesignId);
        return Result.success(urls);
    }
}
