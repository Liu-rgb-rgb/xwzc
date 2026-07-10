package com.xiuwen.pattern.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.pattern.entity.PromptTemplate;
import com.xiuwen.pattern.mapper.PromptTemplateMapper;
import com.xiuwen.pattern.service.PromptTemplateService;
import org.springframework.stereotype.Service;

/**
 * prompt_template 表服务实现。
 */
@Service
public class PromptTemplateServiceImpl extends ServiceImpl<PromptTemplateMapper, PromptTemplate> implements PromptTemplateService {
}
