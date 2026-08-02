package com.xiuwen.web.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.course.entity.Course;
import com.xiuwen.course.entity.LearningResource;
import com.xiuwen.course.service.CourseService;
import com.xiuwen.course.service.LearningResourceService;
import com.xiuwen.pattern.entity.Pattern;
import com.xiuwen.pattern.service.PatternService;
import com.xiuwen.product.entity.Product;
import com.xiuwen.product.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final PatternService patternService;
    private final ProductService productService;
    private final CourseService courseService;
    private final LearningResourceService resourceService;

    public SearchController(PatternService patternService, ProductService productService,
                            CourseService courseService, LearningResourceService resourceService) {
        this.patternService = patternService;
        this.productService = productService;
        this.courseService = courseService;
        this.resourceService = resourceService;
    }

    /** [5.4] 全站搜索 */
    @GetMapping
    public Result<PageResult<Map<String, Object>>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "12") Long pageSize) {

        List<Map<String, Object>> allResults = new ArrayList<>();

        if (type == null || type.isEmpty() || "all".equals(type)) {
            allResults.addAll(searchPatterns(keyword));
            allResults.addAll(searchProducts(keyword));
            allResults.addAll(searchCourses(keyword));
            allResults.addAll(searchResources(keyword));
        } else {
            switch (type) {
                case "pattern":
                    allResults.addAll(searchPatterns(keyword));
                    break;
                case "product":
                    allResults.addAll(searchProducts(keyword));
                    break;
                case "course":
                    allResults.addAll(searchCourses(keyword));
                    break;
                case "resource":
                    allResults.addAll(searchResources(keyword));
                    break;
            }
        }

        long total = allResults.size();
        long from = (page - 1) * pageSize;
        long to = Math.min(from + pageSize, total);
        List<Map<String, Object>> paged = from < total ? allResults.subList((int) from, (int) to) : List.of();

        return Result.success(PageResult.of(total, page, pageSize, paged));
    }

    private List<Map<String, Object>> searchPatterns(String keyword) {
        List<Pattern> list = patternService.list(new LambdaQueryWrapper<Pattern>()
                .eq(Pattern::getStatus, "NORMAL")
                .and(w -> w.like(Pattern::getTitle, keyword).or().like(Pattern::getDescription, keyword))
                .last("LIMIT 50"));
        List<Map<String, Object>> results = new ArrayList<>();
        for (Pattern p : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "pattern");
            item.put("id", p.getId());
            item.put("title", p.getTitle());
            item.put("subtitle", p.getStyle());
            item.put("coverImage", p.getImageUrl());
            item.put("description", p.getDescription());
            results.add(item);
        }
        return results;
    }

    private List<Map<String, Object>> searchProducts(String keyword) {
        List<Product> list = productService.list(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, "ON_SALE")
                .and(w -> w.like(Product::getName, keyword).or().like(Product::getDescription, keyword))
                .last("LIMIT 50"));
        List<Map<String, Object>> results = new ArrayList<>();
        for (Product p : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "product");
            item.put("id", p.getId());
            item.put("title", p.getName());
            item.put("subtitle", p.getSubtitle());
            item.put("coverImage", p.getCoverImage());
            item.put("description", p.getDescription());
            results.add(item);
        }
        return results;
    }

    private List<Map<String, Object>> searchCourses(String keyword) {
        List<Course> list = courseService.list(new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, "PUBLISHED")
                .and(w -> w.like(Course::getTitle, keyword).or().like(Course::getDescription, keyword))
                .last("LIMIT 50"));
        List<Map<String, Object>> results = new ArrayList<>();
        for (Course c : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "course");
            item.put("id", c.getId());
            item.put("title", c.getTitle());
            item.put("subtitle", c.getSubtitle());
            item.put("coverImage", c.getCoverImage());
            item.put("description", c.getDescription());
            results.add(item);
        }
        return results;
    }

    private List<Map<String, Object>> searchResources(String keyword) {
        List<LearningResource> list = resourceService.list(new LambdaQueryWrapper<LearningResource>()
                .eq(LearningResource::getStatus, "PUBLISHED")
                .and(w -> w.like(LearningResource::getTitle, keyword).or().like(LearningResource::getContent, keyword))
                .last("LIMIT 50"));
        List<Map<String, Object>> results = new ArrayList<>();
        for (LearningResource r : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "resource");
            item.put("id", r.getId());
            item.put("title", r.getTitle());
            item.put("subtitle", r.getSubtitle());
            item.put("coverImage", r.getCoverImage());
            item.put("description", r.getContent());
            results.add(item);
        }
        return results;
    }
}
