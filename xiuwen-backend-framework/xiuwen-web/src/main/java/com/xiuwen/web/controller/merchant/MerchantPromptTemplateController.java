package com.xiuwen.web.controller.merchant;
import com.xiuwen.common.core.domain.Result;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

/**
 * ClassName: MerchantPromptTemplateController
 * Package: com.xiuwen.web.controller.merchant
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/13 22:56
 * @Version 1.0
 */
@Data
@RestController(" /api/admin/prompt-templates")
public class MerchantPromptTemplateController {
    @GetMapping
    public Result<Void> getMerchantPromptTemplate(){return Result.todo("查看模板");}
    @PostMapping
    public Result<Void> postMerchantPromptTemplate(){return Result.todo("新增模板");}
    @PutMapping("/{templateId}")
    public Result<Void> putMerchantPromptTemplate(){return Result.todo("编辑模板");}
    @DeleteMapping("/{templateId}")
    public Result<Void> deleteMerchantPromptTemplate(){return Result.todo("删除或停用模板");}

}
