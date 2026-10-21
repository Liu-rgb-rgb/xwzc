package com.xiuwen.pattern.service;

import com.xiuwen.pattern.dto.ProductionOrderCreateRequest;
import com.xiuwen.pattern.entity.ProductionOrder;
import com.xiuwen.pattern.vo.ProductionOrderStatsVO;

import java.util.List;

/**
 * 生产工单服务：客户端提交打样 → 工厂端排产。
 */
public interface ProductionOrderService {

    /** 状态：待生产 */
    String STATUS_PENDING = "PENDING";
    /** 状态：生产中 */
    String STATUS_IN_PRODUCTION = "IN_PRODUCTION";
    /** 状态：已完成 */
    String STATUS_COMPLETED = "COMPLETED";

    /** 客户端创建生产工单（提交给工厂） */
    ProductionOrder create(ProductionOrderCreateRequest request, Long userId);

    /** 工厂端按状态查询工单（status 为空返回全部），按提交时间倒序 */
    List<ProductionOrder> listByStatus(String status);

    /** 用户端查询本人提交的生产工单，按提交时间倒序 */
    List<ProductionOrder> listMine(Long userId);

    /** 工厂端工单状态统计 */
    ProductionOrderStatsVO stats();

    /** 工厂端更新工单状态（开始生产 / 标记完成） */
    ProductionOrder updateStatus(Long id, String status);
}
