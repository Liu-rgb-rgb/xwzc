package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;
import com.xiuwen.framework.security.LoginUserHolder;
import com.xiuwen.pattern.dto.ProductionOrderCreateRequest;
import com.xiuwen.pattern.entity.ProductionOrder;
import com.xiuwen.pattern.service.ProductionOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端"提交给工厂"接口：打版完成后创建生产工单。
 */
@RestController
@RequestMapping("/api/factory-orders")
public class FactoryOrderUserController {

    private final ProductionOrderService productionOrderService;

    public FactoryOrderUserController(ProductionOrderService productionOrderService) {
        this.productionOrderService = productionOrderService;
    }

    /**
     * 提交生产工单（提交打样）。
     * 携带纹样信息、针法、针数、DST 文件地址与动态报价，
     * 乡村工坊端随即出现一条"待接单"任务。
     */
    @PostMapping
    public Result<ProductionOrder> submit(@RequestBody ProductionOrderCreateRequest request) {
        Long userId = LoginUserHolder.getRequiredUserId();
        return Result.success(productionOrderService.create(request, userId));
    }

    /**
     * 我的生产工单：用户端跟踪"待接单/生产中(由XX工坊承接)/已发货"。
     */
    @GetMapping("/mine")
    public Result<java.util.List<ProductionOrder>> mine() {
        Long userId = LoginUserHolder.getRequiredUserId();
        return Result.success(productionOrderService.listMine(userId));
    }
}
