package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/pattern-generations")
public class MerchantPatternGenerationController {
    @GetMapping
    public Result<Void> list() {
        return Result.todo("AI 生成记录列表");
    }
}