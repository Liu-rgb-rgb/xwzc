package com.xiuwen.course.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiuwen.course.entity.Course;

/**
 * course 表服务接口。
 */
public interface CourseService extends IService<Course> {

    Page<Course> pageWithCategory(Page<Course> page, Long categoryId, String keyword,
                                  String difficulty, Boolean isFree);

    Course getByIdWithCategory(Long id);

    /** 商家端课程分页 */
    Page<Course> adminPage(Page<Course> page, Long categoryId, String keyword,
                           String difficulty, String status);
}
