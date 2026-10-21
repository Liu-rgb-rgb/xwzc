package com.xiuwen.pattern.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.pattern.dto.PromptTemplateDTO;
import com.xiuwen.pattern.entity.PromptTemplate;
import com.xiuwen.pattern.mapper.PromptTemplateMapper;
import com.xiuwen.pattern.service.PromptTemplateService;
import com.xiuwen.pattern.vo.PromptTemplateVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * prompt_template 表服务实现。
 */
@Service
public class PromptTemplateServiceImpl extends ServiceImpl<PromptTemplateMapper, PromptTemplate> implements PromptTemplateService {

    @Override
    public List<PromptTemplateVO> listTemplates(String style, Integer status) {
        LambdaQueryWrapper<PromptTemplate> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(style)) {
            queryWrapper.eq(PromptTemplate::getStyle, style);
        }
        if (status != null) {
            queryWrapper.eq(PromptTemplate::getStatus, status);
        }
        queryWrapper.orderByAsc(PromptTemplate::getSort)
                .orderByDesc(PromptTemplate::getCreatedAt);

        List<PromptTemplate> list = list(queryWrapper);
        return list.stream()
                .map(PromptTemplateVO::from)
                .collect(Collectors.toList());
    }

    @Override
    public PromptTemplateVO getTemplateById(Long id) {
        PromptTemplate template = getById(id);
        if (template == null) {
            throw new BusinessException("提示词模板不存在");
        }
        return PromptTemplateVO.from(template);
    }

    @Override
    public PromptTemplateVO createTemplate(PromptTemplateDTO dto) {
        // Validate required fields
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException("模板名称不能为空");
        }
        if (!StringUtils.hasText(dto.getTemplateText())) {
            throw new BusinessException("模板内容不能为空");
        }
        if (!StringUtils.hasText(dto.getStyle())) {
            throw new BusinessException("模板风格不能为空");
        }

        // Check for duplicate template name
        LambdaQueryWrapper<PromptTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PromptTemplate::getName, dto.getName());
        if (count(queryWrapper) > 0) {
            throw new BusinessException("模板名称已存在");
        }

        PromptTemplate template = new PromptTemplate();
        template.setName(dto.getName());
        template.setStyle(dto.getStyle());
        template.setUsageScene(dto.getUsageScene());
        template.setColorTheme(dto.getColorTheme());
        template.setTemplateText(dto.getTemplateText());
        template.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        template.setSort(dto.getSort() != null ? dto.getSort() : 0);

        save(template);
        return PromptTemplateVO.from(template);
    }

    @Override
    public PromptTemplateVO updateTemplate(Long id, PromptTemplateDTO dto) {
        // Validate required fields
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException("模板名称不能为空");
        }
        if (!StringUtils.hasText(dto.getTemplateText())) {
            throw new BusinessException("模板内容不能为空");
        }
        if (!StringUtils.hasText(dto.getStyle())) {
            throw new BusinessException("模板风格不能为空");
        }

        PromptTemplate template = getById(id);
        if (template == null) {
            throw new BusinessException("提示词模板不存在");
        }

        // Check for duplicate template name (excluding current template)
        LambdaQueryWrapper<PromptTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PromptTemplate::getName, dto.getName())
                .ne(PromptTemplate::getId, id);
        if (count(queryWrapper) > 0) {
            throw new BusinessException("模板名称已存在");
        }

        template.setName(dto.getName());
        template.setStyle(dto.getStyle());
        template.setUsageScene(dto.getUsageScene());
        template.setColorTheme(dto.getColorTheme());
        template.setTemplateText(dto.getTemplateText());
        template.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        template.setSort(dto.getSort() != null ? dto.getSort() : 0);

        updateById(template);
        return PromptTemplateVO.from(template);
    }

    @Override
    public void deleteTemplate(Long id) {
        PromptTemplate template = getById(id);
        if (template == null) {
            throw new BusinessException("提示词模板不存在");
        }
        removeById(id);
    }
}
