package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;
import com.xiuwen.pattern.dto.PromptTemplateDTO;
import com.xiuwen.pattern.service.PromptTemplateService;
import com.xiuwen.pattern.vo.PromptTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 商家端提示词模板管理接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/prompt-templates")
@RequiredArgsConstructor
public class MerchantPromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    /**
     * 获取提示词模板列表（支持风格、状态筛选）
     */
    @GetMapping
    public Result<List<PromptTemplateVO>> listTemplates(
            @RequestParam(required = false) String style,
            @RequestParam(required = false) Integer status) {
        List<PromptTemplateVO> list = promptTemplateService.listTemplates(style, status);
        return Result.success(list);
    }

    /**
     * 获取提示词模板详情
     */
    @GetMapping("/{templateId}")
    public Result<PromptTemplateVO> getTemplate(@PathVariable Long templateId) {
        PromptTemplateVO vo = promptTemplateService.getTemplateById(templateId);
        return Result.success(vo);
    }

    /**
     * 新增提示词模板
     */
    @PostMapping
    public Result<PromptTemplateVO> createTemplate(@Valid @RequestBody PromptTemplateDTO dto) {
        PromptTemplateVO vo = promptTemplateService.createTemplate(dto);
        return Result.success(vo);
    }

    /**
     * 编辑提示词模板
     */
    @PutMapping("/{templateId}")
    public Result<PromptTemplateVO> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody PromptTemplateDTO dto) {
        PromptTemplateVO vo = promptTemplateService.updateTemplate(templateId, dto);
        return Result.success(vo);
    }

    /**
     * 删除或停用提示词模板
     */
    @DeleteMapping("/{templateId}")
    public Result<Void> deleteTemplate(@PathVariable Long templateId) {
        promptTemplateService.deleteTemplate(templateId);
        return Result.success();
    }
}
