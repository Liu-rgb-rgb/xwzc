package com.xiuwen.pattern.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.pattern.dto.ProductionOrderCreateRequest;
import com.xiuwen.pattern.entity.ProductionOrder;
import com.xiuwen.pattern.mapper.ProductionOrderMapper;
import com.xiuwen.pattern.service.ProductionOrderService;
import com.xiuwen.pattern.vo.ProductionOrderStatsVO;
import com.xiuwen.system.entity.User;
import com.xiuwen.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 生产工单服务实现。
 */
@Service
@RequiredArgsConstructor
public class ProductionOrderServiceImpl implements ProductionOrderService {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ProductionOrderMapper productionOrderMapper;
    private final UserMapper userMapper;

    @Override
    public ProductionOrder create(ProductionOrderCreateRequest request, Long userId) {
        if (request == null || !StringUtils.hasText(request.getDstFileUrl())) {
            throw new BusinessException("缺少 DST 文件信息，请先完成一键打版");
        }

        ProductionOrder order = new ProductionOrder();
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setUserNickname(resolveNickname(userId));
        order.setPatternId(request.getPatternId());
        order.setPatternTitle(StringUtils.hasText(request.getPatternTitle())
                ? request.getPatternTitle()
                : "AI 非遗纹样");
        order.setPatternImageUrl(request.getPatternImageUrl());
        order.setStyle(request.getStyle());
        order.setStitchType(StringUtils.hasText(request.getStitchType()) ? request.getStitchType() : "OUTLINE");
        order.setFabricType(request.getFabricType());
        order.setDesignHash(request.getDesignHash());
        order.setDstHash(request.getDstHash());
        order.setStitchImageUrl(request.getStitchImageUrl());
        order.setDstFileUrl(request.getDstFileUrl());
        order.setStitchCount(request.getStitchCount() == null ? 0 : request.getStitchCount());
        order.setFileSize(request.getFileSize());
        order.setQuoteAmount(request.getQuoteAmount() == null
                ? BigDecimal.ZERO
                : request.getQuoteAmount().setScale(2, RoundingMode.HALF_UP));
        order.setStatus(STATUS_PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        productionOrderMapper.insert(order);
        return order;
    }

    @Override
    public List<ProductionOrder> listByStatus(String status) {
        LambdaQueryWrapper<ProductionOrder> wrapper = new LambdaQueryWrapper<ProductionOrder>()
                .orderByDesc(ProductionOrder::getCreatedAt)
                .orderByDesc(ProductionOrder::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(ProductionOrder::getStatus, status);
        }
        return productionOrderMapper.selectList(wrapper);
    }

    @Override
    public List<ProductionOrder> listMine(Long userId) {
        return productionOrderMapper.selectList(new LambdaQueryWrapper<ProductionOrder>()
                .eq(ProductionOrder::getUserId, userId)
                .orderByDesc(ProductionOrder::getCreatedAt)
                .orderByDesc(ProductionOrder::getId));
    }

    @Override
    public ProductionOrderStatsVO stats() {
        ProductionOrderStatsVO vo = new ProductionOrderStatsVO();
        vo.setTotal(countByStatus(null));
        vo.setPending(countByStatus(STATUS_PENDING));
        vo.setInProduction(countByStatus(STATUS_IN_PRODUCTION));
        vo.setCompleted(countByStatus(STATUS_COMPLETED));
        return vo;
    }

    @Override
    public ProductionOrder updateStatus(Long id, String status) {
        if (!StringUtils.hasText(status)
                || !(status.equals(STATUS_PENDING)
                || status.equals(STATUS_IN_PRODUCTION)
                || status.equals(STATUS_COMPLETED))) {
            throw new BusinessException("非法的工单状态");
        }
        ProductionOrder order = productionOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }
        order.setStatus(status);
        // 工坊接单（进入生产中）时记录承接工坊，用户端可见"由 XX 工坊承接"
        if (STATUS_IN_PRODUCTION.equals(status) && !StringUtils.hasText(order.getFactoryName())) {
            order.setFactoryName("岭南乡村非遗工坊");
        }
        order.setUpdatedAt(LocalDateTime.now());
        productionOrderMapper.updateById(order);
        return order;
    }

    private Long countByStatus(String status) {
        LambdaQueryWrapper<ProductionOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(ProductionOrder::getStatus, status);
        }
        return productionOrderMapper.selectCount(wrapper);
    }

    private String nextOrderNo() {
        return "PO" + LocalDateTime.now().format(ORDER_NO_FORMAT)
                + "-" + ThreadLocalRandom.current().nextInt(10, 99);
    }

    private String resolveNickname(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user != null && StringUtils.hasText(user.getNickname())) {
                return user.getNickname();
            }
        } catch (Exception ignored) {
            // 查不到用户时不阻塞工单创建
        }
        return "创作者 #" + userId;
    }
}
