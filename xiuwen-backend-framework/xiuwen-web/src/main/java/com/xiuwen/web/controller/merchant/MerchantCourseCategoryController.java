package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/course-categories")
public class MerchantCourseCategoryController {



    @PutMapping("/{categoryId}")
    public Result<Void> updateCategory(
            @PathVariable Long categoryId) {
        return Result.todo("编辑课程分类");
    }

    @DeleteMapping("/{categoryId}")
    public Result<Void> deleteCategory(
            @PathVariable Long categoryId) {
        return Result.todo("删除课程分类");
    }
}
