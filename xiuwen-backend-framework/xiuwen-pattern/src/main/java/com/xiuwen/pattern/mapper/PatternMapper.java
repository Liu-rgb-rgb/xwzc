package com.xiuwen.pattern.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuwen.pattern.entity.Pattern;
import org.apache.ibatis.annotations.Mapper;

/**
 * pattern 表 Mapper。
 */
@Mapper
public interface PatternMapper extends BaseMapper<Pattern> {
}
