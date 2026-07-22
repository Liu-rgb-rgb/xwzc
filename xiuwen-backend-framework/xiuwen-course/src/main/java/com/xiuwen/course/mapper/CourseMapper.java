package com.xiuwen.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiuwen.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * course 表 Mapper。
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    Page<Course> selectPageWithCategory(Page<Course> page,
                                        @Param("categoryId") Long categoryId,
                                        @Param("keyword") String keyword,
                                        @Param("difficulty") String difficulty,
                                        @Param("isFree") Boolean isFree);

    Course selectByIdWithCategory(@Param("id") Long id);
}
