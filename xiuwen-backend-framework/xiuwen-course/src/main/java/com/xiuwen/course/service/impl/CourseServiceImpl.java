package com.xiuwen.course.service.impl;

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
}
