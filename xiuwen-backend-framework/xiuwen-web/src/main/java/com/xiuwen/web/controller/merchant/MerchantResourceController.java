package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;

import org.springframework.web.bind.annotation.*;

/**
 * 商家端创作资源管理接口。
 */
@RestController
@RequestMapping("/api/admin/resources")
public class MerchantResourceController {


    @GetMapping
    public Result<Void> list() { return Result.todo("资源列表"); }

    @PostMapping
    public Result<Void> create() { return Result.todo("新增资源"); }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id) { return Result.todo("编辑资源"); }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { return Result.todo("删除资源"); }

    @GetMapping("/{resourceId}")
    public Result<Void> resourceId(@PathVariable Long resourceId){return Result.todo("商家查看资源详情");}

    @PutMapping("/{resourceId}/status")
    public Result<Void> resourceStatus(@PathVariable Long resourceId){return  Result.todo("修改资源状态");}

}
