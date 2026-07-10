package com.xiuwen.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuwen.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;

/**
 * course 表 Mapper。
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
