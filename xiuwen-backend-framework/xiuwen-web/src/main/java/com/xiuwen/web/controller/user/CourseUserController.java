package com.xiuwen.web.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.course.entity.Course;
import com.xiuwen.course.entity.CourseCategory;
import com.xiuwen.course.service.CourseCategoryService;
import com.xiuwen.course.service.CourseService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端非遗课堂接口。
 */
@RestController
@RequestMapping("/api/courses")
public class CourseUserController {

    @Resource
    private CourseService courseService;

    @Resource
    private CourseCategoryService courseCategoryService;

    /**
     * [12.1] 课程分类
     */
    @GetMapping("/categories")
    public Result<List<CourseCategory>> categories() {
        LambdaQueryWrapper<CourseCategory> wrapper = new LambdaQueryWrapper<CourseCategory>()
                .eq(CourseCategory::getStatus, 1)
                .orderByAsc(CourseCategory::getSort)
                .orderByAsc(CourseCategory::getId);
        return Result.success(courseCategoryService.list(wrapper));
    }

    /**
     * [12.2] 课程列表
     */
    @GetMapping
    public Result<PageResult<Course>> list(
            @RequestParam(required = false) Long id,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize) {
        Long queryCategoryId = categoryId != null ? categoryId : id;
        Page<Course> pageQuery = new Page<>(page, pageSize);
        courseService.pageWithCategory(pageQuery, queryCategoryId, keyword, difficulty, isFree);
        return Result.success(PageResult.of(
                pageQuery.getTotal(),
                pageQuery.getCurrent(),
                pageQuery.getSize(),
                pageQuery.getRecords()));
    }

    /**
     * [12.3] 课程详情
     */
    @GetMapping("/{id}")
    public Result<Course> detail(@PathVariable Long id) {
        Course course = courseService.getByIdWithCategory(id);
        if (course == null) {
            return Result.fail("课程不存在");
        }
        LambdaUpdateWrapper<Course> updateWrapper = new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, id)
                .setSql("view_count = view_count + 1");
        courseService.update(updateWrapper);
        return Result.success(course);
    }

    /**
     * [12.4] 开始学习
     */
    @PostMapping("/{id}/study")
    public Result<Map<String, Object>> study(@PathVariable Long id) {
        Course course = courseService.getById(id);
        if (course == null) {
            return Result.fail("课程不存在");
        }
        LambdaUpdateWrapper<Course> updateWrapper = new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, id)
                .setSql("study_count = study_count + 1");
        courseService.update(updateWrapper);

        Course updated = courseService.getById(id);
        Map<String, Object> data = new HashMap<>();
        data.put("courseId", id);
        data.put("studyCount", updated.getStudyCount());
        data.put("studiedAt", LocalDateTime.now().toString());
        return Result.success(data);
    }
}
