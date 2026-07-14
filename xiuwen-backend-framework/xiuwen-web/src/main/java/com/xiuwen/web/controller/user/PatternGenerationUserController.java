package com.xiuwen.web.controller.user;


import com.xiuwen.common.core.domain.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pattern-generations")
public class PatternGenerationUserController {

    @GetMapping("/my")
    public Result<Void> myGenerationList() {return Result.todo("分页查询当前用户AI生成记录");}
}