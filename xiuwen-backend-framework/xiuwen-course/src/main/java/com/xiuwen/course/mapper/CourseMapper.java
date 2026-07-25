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

    /** 商家端课程分页（含全部状态） */
    Page<Course> selectAdminPage(Page<Course> page,
                                 @Param("categoryId") Long categoryId,
                                 @Param("keyword") String keyword,
                                 @Param("difficulty") String difficulty,
                                 @Param("status") String status);

    Course selectByIdWithCategory(@Param("id") Long id);
}
