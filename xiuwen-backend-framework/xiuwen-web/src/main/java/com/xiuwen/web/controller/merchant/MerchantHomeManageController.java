package com.xiuwen.web.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.system.dto.BannerSaveRequest;
import com.xiuwen.system.dto.RecommendSaveRequest;
import com.xiuwen.system.entity.HomeBanner;
import com.xiuwen.system.entity.HomeRecommend;
import com.xiuwen.system.service.HomeBannerService;
import com.xiuwen.system.service.HomeRecommendService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/admin/home")
public class MerchantHomeManageController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HomeBannerService bannerService;
    private final HomeRecommendService recommendService;

    public MerchantHomeManageController(HomeBannerService bannerService, HomeRecommendService recommendService) {
        this.bannerService = bannerService;
        this.recommendService = recommendService;
    }

    // ==================== Banner管理 ====================

    /** [12.1] Banner列表 */
    @GetMapping("/banners")
    public Result<PageResult<HomeBanner>> banners(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {
        LambdaQueryWrapper<HomeBanner> query = new LambdaQueryWrapper<HomeBanner>()
                .orderByAsc(HomeBanner::getSort);
        if (status != null) {
            query.eq(HomeBanner::getStatus, status);
        }
        Page<HomeBanner> pageParam = new Page<>(page, pageSize);
        Page<HomeBanner> result = bannerService.page(pageParam, query);
        return Result.success(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /** [12.2] 新增Banner */
    @PostMapping("/banners")
    public Result<HomeBanner> addBanner(@Valid @RequestBody BannerSaveRequest request) {
        HomeBanner banner = new HomeBanner();
        BeanUtils.copyProperties(request, banner);
        if (banner.getSort() == null) {
            banner.setSort(0);
        }
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        bannerService.save(banner);
        return Result.success(banner);
    }

    /** [12.3] 编辑Banner */
    @PutMapping("/banners/{id}")
    public Result<HomeBanner> updateBanner(@PathVariable Long id, @Valid @RequestBody BannerSaveRequest request) {
        HomeBanner existing = bannerService.getById(id);
        if (existing == null) {
            throw new BusinessException("Banner不存在");
        }
        HomeBanner banner = new HomeBanner();
        BeanUtils.copyProperties(request, banner);
        banner.setId(id);
        bannerService.updateById(banner);
        return Result.success(bannerService.getById(id));
    }

    /** [12.4] 删除Banner */
    @DeleteMapping("/banners/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        HomeBanner existing = bannerService.getById(id);
        if (existing == null) {
            throw new BusinessException("Banner不存在");
        }
        bannerService.removeById(id);
        return Result.success();
    }

    // ==================== 推荐位管理 ====================

    /** [12.5] 推荐位列表 */
    @GetMapping("/recommend")
    public Result<PageResult<HomeRecommend>> recommendList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {
        LambdaQueryWrapper<HomeRecommend> query = new LambdaQueryWrapper<HomeRecommend>()
                .orderByAsc(HomeRecommend::getSort);
        if (type != null && !type.isEmpty()) {
            query.eq(HomeRecommend::getRecommendType, type);
        }
        if (status != null) {
            query.eq(HomeRecommend::getStatus, status);
        }
        Page<HomeRecommend> pageParam = new Page<>(page, pageSize);
        Page<HomeRecommend> result = recommendService.page(pageParam, query);
        return Result.success(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /** [12.6] 新增推荐内容 */
    @PostMapping("/recommend")
    public Result<HomeRecommend> addRecommend(@Valid @RequestBody RecommendSaveRequest request) {
        HomeRecommend recommend = new HomeRecommend();
        BeanUtils.copyProperties(request, recommend, "startAt", "endAt");
        if (recommend.getSort() == null) {
            recommend.setSort(0);
        }
        if (recommend.getStatus() == null) {
            recommend.setStatus(1);
        }
        if (request.getStartAt() != null && !request.getStartAt().isEmpty()) {
            recommend.setStartAt(LocalDateTime.parse(request.getStartAt(), DT_FMT));
        }
        if (request.getEndAt() != null && !request.getEndAt().isEmpty()) {
            recommend.setEndAt(LocalDateTime.parse(request.getEndAt(), DT_FMT));
        }
        recommendService.save(recommend);
        return Result.success(recommend);
    }

    /** [12.7] 编辑推荐内容 */
    @PutMapping("/recommend/{id}")
    public Result<HomeRecommend> updateRecommend(@PathVariable Long id, @Valid @RequestBody RecommendSaveRequest request) {
        HomeRecommend existing = recommendService.getById(id);
        if (existing == null) {
            throw new BusinessException("推荐内容不存在");
        }
        HomeRecommend recommend = new HomeRecommend();
        BeanUtils.copyProperties(request, recommend, "startAt", "endAt");
        recommend.setId(id);
        if (request.getStartAt() != null && !request.getStartAt().isEmpty()) {
            recommend.setStartAt(LocalDateTime.parse(request.getStartAt(), DT_FMT));
        }
        if (request.getEndAt() != null && !request.getEndAt().isEmpty()) {
            recommend.setEndAt(LocalDateTime.parse(request.getEndAt(), DT_FMT));
        }
        recommendService.updateById(recommend);
        return Result.success(recommendService.getById(id));
    }

    /** [12.8] 删除推荐内容 */
    @DeleteMapping("/recommend/{id}")
    public Result<Void> deleteRecommend(@PathVariable Long id) {
        HomeRecommend existing = recommendService.getById(id);
        if (existing == null) {
            throw new BusinessException("推荐内容不存在");
        }
        recommendService.removeById(id);
        return Result.success();
    }
}
