package com.xiuwen.web.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.system.entity.HomeBanner;
import com.xiuwen.system.entity.HomeRecommend;
import com.xiuwen.system.service.HomeBannerService;
import com.xiuwen.system.service.HomeRecommendService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeBannerService bannerService;
    private final HomeRecommendService recommendService;

    public HomeController(HomeBannerService bannerService, HomeRecommendService recommendService) {
        this.bannerService = bannerService;
        this.recommendService = recommendService;
    }

    /** [5.1] 首页聚合数据 */
    @GetMapping
    public Result<Map<String, Object>> home() {
        List<HomeBanner> banners = bannerService.list(
                new LambdaQueryWrapper<HomeBanner>()
                        .eq(HomeBanner::getStatus, 1)
                        .orderByAsc(HomeBanner::getSort));

        List<HomeRecommend> allRecommends = recommendService.list(
                new LambdaQueryWrapper<HomeRecommend>()
                        .eq(HomeRecommend::getStatus, 1)
                        .orderByAsc(HomeRecommend::getSort));

        Map<String, List<HomeRecommend>> grouped = allRecommends.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getRecommendType() != null ? r.getRecommendType() : "OTHER",
                        LinkedHashMap::new, Collectors.toList()));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("banners", banners);
        data.put("recommendPatterns", grouped.getOrDefault("PATTERN", List.of()));
        data.put("recommendProducts", grouped.getOrDefault("PRODUCT", List.of()));
        data.put("recommendCourses", grouped.getOrDefault("COURSE", List.of()));
        data.put("recommendResources", grouped.getOrDefault("RESOURCE", List.of()));
        return Result.success(data);
    }

    /** [5.2] Banner列表 */
    @GetMapping("/banners")
    public Result<List<HomeBanner>> banners() {
        List<HomeBanner> list = bannerService.list(
                new LambdaQueryWrapper<HomeBanner>()
                        .eq(HomeBanner::getStatus, 1)
                        .orderByAsc(HomeBanner::getSort));
        return Result.success(list);
    }

    /** [5.3] 推荐位内容 */
    @GetMapping("/recommend")
    public Result<List<HomeRecommend>> recommend(@RequestParam(required = false) String type) {
        LambdaQueryWrapper<HomeRecommend> query = new LambdaQueryWrapper<HomeRecommend>()
                .eq(HomeRecommend::getStatus, 1)
                .orderByAsc(HomeRecommend::getSort);
        if (type != null && !type.isEmpty()) {
            query.eq(HomeRecommend::getRecommendType, type);
        }
        return Result.success(recommendService.list(query));
    }
}
