package com.xiuwen.web.controller.merchant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.course.dto.CourseCategoryRequest;
import com.xiuwen.course.entity.CourseCategory;
import com.xiuwen.course.service.CourseCategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/course-categories")
public class MerchantCourseCategoryController {

    @Autowired
    private CourseCategoryService courseCategoryService;

    /** [9.1] 课程分类列表 */
    @GetMapping
    public Result<PageResult<CourseCategory>> list(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<CourseCategory> pageParam = new Page<>(page, pageSize);
        Page<CourseCategory> result = courseCategoryService.page(pageParam);
        return Result.success(PageResult.of(
                result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /** [9.2] 新增课程分类 */
    @PostMapping
    public Result<CourseCategory> create(@Valid @RequestBody CourseCategoryRequest request) {
        CourseCategory category = new CourseCategory();
        BeanUtils.copyProperties(request, category);
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        courseCategoryService.save(category);
        return Result.success(category);
    }

    /** [9.3] 编辑课程分类 */
    @PutMapping("/{categoryId}")
    public Result<CourseCategory> updateCategory(@PathVariable Long categoryId,
                                                  @Valid @RequestBody CourseCategoryRequest request) {
        CourseCategory existing = courseCategoryService.getById(categoryId);
        if (existing == null) {
            throw new BusinessException("课程分类不存在");
        }
        CourseCategory category = new CourseCategory();
        BeanUtils.copyProperties(request, category);
        category.setId(categoryId);
        courseCategoryService.updateById(category);
        CourseCategory updated = courseCategoryService.getById(categoryId);
        return Result.success(updated);
    }

    /** [9.4] 删除或禁用课程分类 */
    @DeleteMapping("/{categoryId}")
    public Result<Void> deleteCategory(@PathVariable Long categoryId) {
        CourseCategory existing = courseCategoryService.getById(categoryId);
        if (existing == null) {
            throw new BusinessException("课程分类不存在");
        }
        courseCategoryService.removeById(categoryId);
        return Result.success();
    }
}
