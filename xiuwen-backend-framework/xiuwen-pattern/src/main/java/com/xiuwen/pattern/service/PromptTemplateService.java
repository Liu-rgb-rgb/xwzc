package com.xiuwen.pattern.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiuwen.pattern.dto.PromptTemplateDTO;
import com.xiuwen.pattern.entity.PromptTemplate;
import com.xiuwen.pattern.vo.PromptTemplateVO;

import java.util.List;

/**
 * prompt_template 表服务接口。
 */
public interface PromptTemplateService extends IService<PromptTemplate> {

    /**
     * 获取提示词模板列表（商家端）
     */
    List<PromptTemplateVO> listTemplates(String style, Integer status);

    /**
     * 获取提示词模板详情
     */
    PromptTemplateVO getTemplateById(Long id);

    /**
     * 创建提示词模板
     */
    PromptTemplateVO createTemplate(PromptTemplateDTO dto);

    /**
     * 更新提示词模板
     */
    PromptTemplateVO updateTemplate(Long id, PromptTemplateDTO dto);

    /**
     * 删除提示词模板
     */
    void deleteTemplate(Long id);
}
