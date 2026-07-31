package com.xiuwen.web.controller.merchant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.order.dto.OrderRemarkDTO;
import com.xiuwen.order.dto.OrderStatusUpdateDTO;
import com.xiuwen.order.entity.OrderDetail;
import com.xiuwen.order.entity.Orders;
import com.xiuwen.order.service.OrderService;
import com.xiuwen.order.vo.OrderStatusCountVO;
import com.xiuwen.order.vo.OrderVO;
import com.xiuwen.system.entity.User;
import com.xiuwen.system.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商家端订单管理接口。
 */
@RestController
@RequestMapping("/api/admin/orders")
public class MerchantOrderController {

    private final OrderService orderService;
    private final UserService userService;

    public MerchantOrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    /**
     * 2.1 订单列表（支持状态、关键词、时间筛选）
     */
    @GetMapping
    public Result<PageResult<OrderVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false, defaultValue = "1") long page,
            @RequestParam(required = false, defaultValue = "10") long pageSize) {

        IPage<OrderDetail> pageResult = orderService.pageAdminOrders(
                page, pageSize, status, keyword, startTime, endTime);

        // 批量获取用户昵称
        List<OrderDetail> records = pageResult.getRecords();
        Map<Long, String> nicknameMap = batchGetUserNicknames(records);

        // 转换为 VO 并填充用户昵称
        List<OrderVO> voList = new ArrayList<>();
        for (OrderDetail detail : records) {
            OrderVO vo = OrderVO.fromDetail(detail);
            vo.setUserNickname(nicknameMap.get(detail.getUserId()));
            voList.add(vo);
        }

        return Result.success(PageResult.of(
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize(),
                voList
        ));
    }

    /**
     * 2.2 订单详情
     */
    @GetMapping("/{orderId}")
    public Result<OrderVO> detail(@PathVariable Long orderId) {
        OrderDetail detail = orderService.getAdminOrderDetail(orderId);
        OrderVO vo = OrderVO.fromDetail(detail);

        // 获取用户昵称
        if (detail.getUserId() != null) {
            User user = userService.getById(detail.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname());
            }
        }

        return Result.success(vo);
    }

    /**
     * 2.3 修改订单状态（接单 → 制作中 → 待发货 → 已发货）
     */
    @PutMapping("/{orderId}/status")
    public Result<OrderVO> updateStatus(@PathVariable Long orderId,
                                        @Valid @RequestBody OrderStatusUpdateDTO dto) {
        Orders order = orderService.updateOrderStatus(orderId, dto.getStatus());
        return Result.success(OrderVO.from(order));
    }

    /**
     * 2.4 修改商家备注
     */
    @PutMapping("/{orderId}/remark")
    public Result<Void> updateOrderRemark(@PathVariable Long orderId,
                                          @Valid @RequestBody OrderRemarkDTO dto) {
        orderService.updateMerchantRemark(orderId, dto.getRemark());
        return Result.success();
    }

    /**
     * 2.5 商家端订单各状态数量统计
     */
    @GetMapping("/status-count")
    public Result<OrderStatusCountVO> statusCount() {
        return Result.success(orderService.getAdminOrderStatusCount());
    }

    // =============== 私有辅助方法 ===============

    /**
     * 批量获取用户昵称，避免 N+1 查询
     */
    private Map<Long, String> batchGetUserNicknames(List<OrderDetail> records) {
        Set<Long> userIds = records.stream()
                .map(OrderDetail::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickname() != null ? u.getNickname() : ""));
    }
}
