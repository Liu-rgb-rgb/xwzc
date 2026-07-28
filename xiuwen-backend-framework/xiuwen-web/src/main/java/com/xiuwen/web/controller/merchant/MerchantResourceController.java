package com.xiuwen.web.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.course.dto.ResourceSaveRequest;
import com.xiuwen.course.dto.ResourceStatusRequest;
import com.xiuwen.course.entity.LearningResource;
import com.xiuwen.course.service.LearningResourceService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/resources")
public class MerchantResourceController {

    @Autowired
    private LearningResourceService learningResourceService;

    /** [11.1] 创作资源列表 */
    @GetMapping
    public Result<PageResult<LearningResource>> list(@RequestParam(required = false) String type,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Long id,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "1") Long page,
                                                      @RequestParam(defaultValue = "10") Long pageSize) {
        LambdaQueryWrapper<LearningResource> query = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            query.eq(LearningResource::getResourceType, type);
        }
        if (keyword != null && !keyword.isEmpty()) {
            query.like(LearningResource::getTitle, keyword);
        }
        if (id != null) {
            query.eq(LearningResource::getCourseId, id);
        }
        if (status != null && !status.isEmpty()) {
            query.eq(LearningResource::getStatus, status);
        }
        query.orderByDesc(LearningResource::getSort);
        query.orderByDesc(LearningResource::getCreatedAt);

        Page<LearningResource> pageParam = new Page<>(page, pageSize);
        Page<LearningResource> result = learningResourceService.page(pageParam, query);
        return Result.success(PageResult.of(
                result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /** [11.2] 新增创作资源 */
    @PostMapping
    public Result<LearningResource> create(@Valid @RequestBody ResourceSaveRequest request) {
        LearningResource resource = new LearningResource();
        BeanUtils.copyProperties(request, resource);
        if (resource.getSort() == null) {
            resource.setSort(0);
        }
        if (resource.getIsRecommend() == null) {
            resource.setIsRecommend(0);
        }
        if (resource.getStatus() == null) {
            resource.setStatus("DRAFT");
        }
        learningResourceService.save(resource);
        return Result.success(resource);
    }

    /** [11.3] 创作资源详情 */
    @GetMapping("/{resourceId}")
    public Result<LearningResource> detail(@PathVariable Long resourceId) {
        LearningResource resource = learningResourceService.getById(resourceId);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        return Result.success(resource);
    }

    /** [11.4] 编辑创作资源 */
    @PutMapping("/{resourceId}")
    public Result<LearningResource> update(@PathVariable Long resourceId,
                                            @Valid @RequestBody ResourceSaveRequest request) {
        LearningResource existing = learningResourceService.getById(resourceId);
        if (existing == null) {
            throw new BusinessException("资源不存在");
        }
        LearningResource resource = new LearningResource();
        BeanUtils.copyProperties(request, resource);
        resource.setId(resourceId);
        learningResourceService.updateById(resource);
        LearningResource updated = learningResourceService.getById(resourceId);
        return Result.success(updated);
    }

    /** [11.5] 发布、隐藏、草稿状态切换 */
    @PutMapping("/{resourceId}/status")
    public Result<Map<String, Object>> updateStatus(@PathVariable Long resourceId,
                                                     @Valid @RequestBody ResourceStatusRequest request) {
        LearningResource existing = learningResourceService.getById(resourceId);
        if (existing == null) {
            throw new BusinessException("资源不存在");
        }
        String newStatus = request.getStatus();
        if (!"DRAFT".equals(newStatus)
                && !"PUBLISHED".equals(newStatus)
                && !"HIDDEN".equals(newStatus)) {
            throw new BusinessException("无效的资源状态：" + newStatus);
        }
        LearningResource update = new LearningResource();
        update.setId(resourceId);
        update.setStatus(newStatus);
        learningResourceService.updateById(update);
        LearningResource updated = learningResourceService.getById(resourceId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("resourceId", updated.getId());
        data.put("status", updated.getStatus());
        data.put("updatedAt", updated.getUpdatedAt());
        return Result.success(data);
    }

    /** [11.6] 删除创作资源 */
    @DeleteMapping("/{resourceId}")
    public Result<Void> delete(@PathVariable Long resourceId) {
        LearningResource existing = learningResourceService.getById(resourceId);
        if (existing == null) {
            throw new BusinessException("资源不存在");
        }
        learningResourceService.removeById(resourceId);
        return Result.success();
    }
}
