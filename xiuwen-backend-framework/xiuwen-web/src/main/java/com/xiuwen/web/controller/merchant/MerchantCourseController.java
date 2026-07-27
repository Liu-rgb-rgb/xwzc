package com.xiuwen.web.controller.merchant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiuwen.common.constant.CourseStatus;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.course.dto.CourseSaveRequest;
import com.xiuwen.course.dto.CourseStatusRequest;
import com.xiuwen.course.entity.Course;
import com.xiuwen.course.service.CourseService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 商家端课程管理接口。
 */
@RestController
@RequestMapping("/api/admin/courses")
public class MerchantCourseController {

    @Autowired
    private CourseService courseService;

    /** [10.1] 课程列表 */
    @GetMapping
    public Result<PageResult<Course>> list(@RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String difficulty,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "1") Long page,
                                           @RequestParam(defaultValue = "10") Long pageSize) {
        Page<Course> pageParam = new Page<>(page, pageSize);
        Page<Course> result = courseService.adminPage(pageParam, categoryId, keyword, difficulty, status);
        PageResult<Course> pageResult = PageResult.of(
                result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
        return Result.success(pageResult);
    }

    /** [10.2] 新增课程 */
    @PostMapping
    public Result<Course> create(@Valid @RequestBody CourseSaveRequest request) {
        Course course = new Course();
        BeanUtils.copyProperties(request, course);
        if (course.getStatus() == null) {
            course.setStatus(CourseStatus.DRAFT);
        }
        if (course.getSort() == null) {
            course.setSort(0);
        }
        if (course.getIsRecommend() == null) {
            course.setIsRecommend(0);
        }
        courseService.save(course);
        Course created = courseService.getByIdWithCategory(course.getId());
        return Result.success(created);
    }

    /** [10.3] 课程详情 */
    @GetMapping("/{courseId}")
    public Result<Course> detail(@PathVariable Long courseId) {
        Course course = courseService.getByIdWithCategory(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return Result.success(course);
    }

    /** [10.4] 编辑课程 */
    @PutMapping("/{courseId}")
    public Result<Course> update(@PathVariable Long courseId,
                                  @Valid @RequestBody CourseSaveRequest request) {
        Course existing = courseService.getById(courseId);
        if (existing == null) {
            throw new BusinessException("课程不存在");
        }
        Course course = new Course();
        BeanUtils.copyProperties(request, course);
        course.setId(courseId);
        courseService.updateById(course);
        Course updated = courseService.getByIdWithCategory(courseId);
        return Result.success(updated);
    }

    /** [10.5] 发布、隐藏、草稿状态切换 */
    @PutMapping("/{courseId}/status")
    public Result<Map<String, Object>> updateStatus(@PathVariable Long courseId,
                                                     @Valid @RequestBody CourseStatusRequest request) {
        Course existing = courseService.getById(courseId);
        if (existing == null) {
            throw new BusinessException("课程不存在");
        }
        String newStatus = request.getStatus();
        if (!CourseStatus.DRAFT.equals(newStatus)
                && !CourseStatus.PUBLISHED.equals(newStatus)
                && !CourseStatus.HIDDEN.equals(newStatus)) {
            throw new BusinessException("无效的课程状态：" + newStatus);
        }
        Course update = new Course();
        update.setId(courseId);
        update.setStatus(newStatus);
        courseService.updateById(update);
        Course updated = courseService.getById(courseId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("courseId", updated.getId());
        data.put("status", updated.getStatus());
        data.put("updatedAt", updated.getUpdatedAt());
        return Result.success(data);
    }

    /** [10.6] 删除课程 */
    @DeleteMapping("/{courseId}")
    public Result<Void> delete(@PathVariable Long courseId) {
        Course existing = courseService.getById(courseId);
        if (existing == null) {
            throw new BusinessException("课程不存在");
        }
        courseService.removeById(courseId);
        return Result.success();
    }
}
