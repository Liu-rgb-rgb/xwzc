package com.xiuwen.pattern.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiuwen.pattern.entity.Pattern;
import com.xiuwen.pattern.entity.PatternAdminDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * pattern 表 Mapper。
 */
@Mapper
public interface PatternMapper extends BaseMapper<Pattern> {

    /**
     * 商家端纹样分页列表（关联 user 表获取昵称）
     */
    IPage<PatternAdminDetail> selectAdminPatternPage(IPage<Object> page,
                                                     @Param("userId") Long userId,
                                                     @Param("keyword") String keyword,
                                                     @Param("style") String style,
                                                     @Param("status") String status,
                                                     @Param("isRecommend") Integer isRecommend);
}
