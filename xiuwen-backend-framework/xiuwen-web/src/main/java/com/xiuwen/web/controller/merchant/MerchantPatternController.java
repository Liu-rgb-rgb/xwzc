package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 商家端纹样管理接口。
 */
@RestController
@RequestMapping("/api/admin/patterns")
public class MerchantPatternController {


    @GetMapping
    public Result<Void> list() { return Result.todo("纹样管理列表"); }


    @GetMapping("/{id}")
    public Result<Void> detail(@PathVariable Long id) { return Result.todo("纹样详情"); }

    @PutMapping("/{id}/recommend")
    public Result<Void> recommend(@PathVariable Long id) { return Result.todo("设置纹样推荐"); }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id) { return Result.todo("隐藏或恢复纹样"); }



    @DeleteMapping("/{patternId}")
    public Result<Void> deletePatternId(@PathVariable Long patternId){return  Result.todo("商家删除`纹样");}

}
