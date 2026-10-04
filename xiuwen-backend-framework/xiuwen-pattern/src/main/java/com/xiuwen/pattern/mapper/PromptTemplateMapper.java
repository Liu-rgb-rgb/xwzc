package com.xiuwen.pattern.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuwen.pattern.entity.PromptTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * prompt_template 表 Mapper。
 */
@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplate> {
}
