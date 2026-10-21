package com.xiuwen.pattern.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuwen.pattern.entity.ProductionOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * production_order 表 Mapper。
 */
@Mapper
public interface ProductionOrderMapper extends BaseMapper<ProductionOrder> {
}
