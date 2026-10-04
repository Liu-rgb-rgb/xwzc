package com.xiuwen.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.course.entity.CourseCategory;
import com.xiuwen.course.mapper.CourseCategoryMapper;
import com.xiuwen.course.service.CourseCategoryService;
import org.springframework.stereotype.Service;

/**
 * course_category 表服务实现。
 */
@Service
public class CourseCategoryServiceImpl extends ServiceImpl<CourseCategoryMapper, CourseCategory> implements CourseCategoryService {
}
