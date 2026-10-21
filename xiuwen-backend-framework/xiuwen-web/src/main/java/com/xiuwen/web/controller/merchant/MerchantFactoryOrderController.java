package com.xiuwen.web.controller.merchant;

import com.xiuwen.common.core.domain.Result;
import com.xiuwen.pattern.entity.ProductionOrder;
import com.xiuwen.pattern.service.ProductionOrderService;
import com.xiuwen.pattern.vo.ProductionOrderStatsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工厂/绣娘端生产工单接口（商家后台鉴权，MERCHANT_ADMIN / ADMIN）。
 */
@RestController
@RequestMapping("/api/merchant/factory/orders")
public class MerchantFactoryOrderController {

    private final ProductionOrderService productionOrderService;

    public MerchantFactoryOrderController(ProductionOrderService productionOrderService) {
        this.productionOrderService = productionOrderService;
    }

    /**
     * 工单列表（可按状态过滤：PENDING / IN_PRODUCTION / COMPLETED）。
     */
    @GetMapping
    public Result<List<ProductionOrder>> list(@RequestParam(required = false) String status) {
        return Result.success(productionOrderService.listByStatus(status));
    }

    /**
     * 工单状态统计（工厂端看板）。
     */
    @GetMapping("/stats")
    public Result<ProductionOrderStatsVO> stats() {
        return Result.success(productionOrderService.stats());
    }

    /**
     * 更新工单状态：开始生产 / 标记完成。
     */
    @PutMapping("/{id}/status")
    public Result<ProductionOrder> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return Result.success(productionOrderService.updateStatus(id, status));
    }
}
