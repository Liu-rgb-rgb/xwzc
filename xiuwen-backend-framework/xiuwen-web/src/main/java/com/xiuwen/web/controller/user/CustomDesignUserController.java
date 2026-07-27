package com.xiuwen.web.controller.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;

import com.xiuwen.framework.security.LoginUserHolder;
import com.xiuwen.product.dto.CustomDesignDTO;
import com.xiuwen.product.entity.CustomDesign;
import com.xiuwen.product.entity.CustomDesignDetail;
import com.xiuwen.product.service.CustomDesignService;
import com.xiuwen.product.vo.CustomDesignVO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户端商品定制接口。
 */
@RestController
@RequestMapping("/api/custom-designs")
public class CustomDesignUserController {
    private final CustomDesignService customDesignService;

    public CustomDesignUserController(CustomDesignService customDesignService) {
        this.customDesignService = customDesignService;
    }
//todo 到时合并要修改
////创建商品定制预览
//    @PostMapping
//    public Result<Void> create() { return Result.todo("创建定制预览"); }
//// 我的定制方案列表
//    @GetMapping("/my")
//    public Result<Void> myList() { return Result.todo("我的定制列表"); }
////定制方案详情
//    @GetMapping("/{id}")
//    public Result<Void> detail(@PathVariable Long id) { return Result.todo("定制详情"); }
//// 删除定制方案
//    @DeleteMapping("/{id}")
//    public Result<Void> delete(@PathVariable Long id) { return Result.todo("删除定制方案"); }
//
//    @DeleteMapping("/{customDesignId}")
//    public Result<Void> deleteCustomDesignId(@PathVariable Long customDesignId){return Result.todo("删除我的定制");}


    //创建商品定制预览
    @PostMapping
    public Result<CustomDesignVO> createDesign(@Valid @RequestBody CustomDesignDTO customDesignDTO) {
     Long userId = LoginUserHolder.getRequiredUserId();
     CustomDesign design = customDesignService.createDesignDetail(
             userId,
             customDesignDTO.getProductId(),
             customDesignDTO.getPatternId(),
             customDesignDTO.getDesignConfig(),
             customDesignDTO.getRemark()
     );
        return Result.success(CustomDesignVO.fromDetail(design));
    }
    // 我的定制方案列表
    @GetMapping("/my")
    public Result<PageResult<CustomDesignVO>> listMyDesign(
            @RequestParam(required = false,defaultValue = "1") int page,
            @RequestParam(required = false,defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        Long userId = LoginUserHolder.getRequiredUserId();
        IPage<CustomDesign> pageReult = customDesignService.pageMyDesign(userId,page,pageSize,status);
       List<CustomDesignVO> voList = pageReult.getRecords().stream().map(design ->{
            CustomDesignDetail detail = customDesignService.getDesignDetail(design.getId());
            return CustomDesignVO.fromDetail(detail != null ? detail : design);
        }).toList();

       PageResult<CustomDesignVO> result = new PageResult<>();
       result.setTotal(pageReult.getTotal());
       result.setPage(page);
       result.setPageSize(pageSize);
       result.setList(voList);
        return Result.success(result);
    }
    //定制方案详情
    @GetMapping("/{customDesignId}")
    public Result<CustomDesignVO> detail(@PathVariable Long customDesignId) {
        Long userId = LoginUserHolder.getRequiredUserId();
        CustomDesign customDesign = customDesignService.getDesignDetail(customDesignId);
        if(customDesign == null || !customDesign.getUserId().equals(userId)){
            return Result.fail("定制方案不存在");
        }
        return Result.success(CustomDesignVO.fromDetail(customDesign));
    }
    // 删除定制方案
    @DeleteMapping("/{customDesignId}")
    public Result<Void> delete(@PathVariable Long customDesignId) {
        Long userId = LoginUserHolder.getRequiredUserId();
        customDesignService.deletedDesign(userId,customDesignId);
        return Result.success();
    }

}
