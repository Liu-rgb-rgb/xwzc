package com.xiuwen.web.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.course.entity.LearningResource;
import com.xiuwen.course.service.LearningResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
public class ResourceUserController {

    @Autowired
    private LearningResourceService learningResourceService;

    /** [13.1] 创作资源列表 */
    @GetMapping
    public Result<PageResult<LearningResource>> list(@RequestParam(required = false) String type,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Long id,
                                                      @RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "12") Integer pageSize) {
        LambdaQueryWrapper<LearningResource> query = new LambdaQueryWrapper<>();
        query.eq(LearningResource::getStatus, "PUBLISHED");
        if (type != null && !type.isEmpty()) {
            query.eq(LearningResource::getResourceType, type);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.like(LearningResource::getTitle, keyword);
        }
        if (id != null) {
            query.eq(LearningResource::getCourseId, id);
        }
        query.orderByDesc(LearningResource::getSort);
        query.orderByDesc(LearningResource::getCreatedAt);

        Page<LearningResource> pageParam = new Page<>(page, pageSize);
        Page<LearningResource> result = learningResourceService.page(pageParam, query);
        return Result.success(PageResult.of(
                result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /** [13.2] 创作资源详情 */
    @GetMapping("/{resourceId}")
    public Result<LearningResource> detail(@PathVariable Long resourceId) {
        LearningResource resource = learningResourceService.getById(resourceId);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        return Result.success(resource);
    }

    /** [13.3] 下载资源 */
    @GetMapping("/{resourceId}/download")
    public Result<LearningResource> download(@PathVariable Long resourceId) {
        LearningResource resource = learningResourceService.getById(resourceId);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        LearningResource update = new LearningResource();
        update.setId(resourceId);
        update.setDownloadCount(resource.getDownloadCount() != null
                ? resource.getDownloadCount() + 1 : 1);
        learningResourceService.updateById(update);
        resource.setDownloadCount(update.getDownloadCount());
        return Result.success(resource);
    }
}
