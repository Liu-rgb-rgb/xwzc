package com.xiuwen.course.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.course.entity.Course;
import com.xiuwen.course.mapper.CourseMapper;
import com.xiuwen.course.service.CourseService;
import org.springframework.stereotype.Service;

/**
 * course 表服务实现。
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    @Override
    public Page<Course> pageWithCategory(Page<Course> page, Long categoryId, String keyword,
                                         String difficulty, Boolean isFree) {
        return baseMapper.selectPageWithCategory(page, categoryId, keyword, difficulty, isFree);
    }

    @Override
    public Course getByIdWithCategory(Long id) {
        return baseMapper.selectByIdWithCategory(id);
    }

    @Override
    public Page<Course> adminPage(Page<Course> page, Long categoryId, String keyword,
                                   String difficulty, String status) {
        return baseMapper.selectAdminPage(page, categoryId, keyword, difficulty, status);
    }
}
