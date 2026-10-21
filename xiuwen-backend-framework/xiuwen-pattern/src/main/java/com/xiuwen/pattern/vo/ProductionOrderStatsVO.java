package com.xiuwen.pattern.vo;

import lombok.Data;

/**
 * 工厂端工单状态统计。
 */
@Data
public class ProductionOrderStatsVO {
    private Long total = 0L;
    private Long pending = 0L;
    private Long inProduction = 0L;
    private Long completed = 0L;
}
