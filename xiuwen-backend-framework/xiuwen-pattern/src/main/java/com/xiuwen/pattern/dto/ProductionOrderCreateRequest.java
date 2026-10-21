package com.xiuwen.pattern.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 客户端"提交给工厂"创建生产工单请求。
 */
@Data
public class ProductionOrderCreateRequest {
    private Long patternId;
    private String patternTitle;
    private String patternImageUrl;
    private String style;
    /** 针法：OUTLINE / SATIN / TATAMI */
    private String stitchType;
    /** 面料：canvas 帆布 / cotton 棉布 / silk 丝绸 */
    private String fabricType;
    /** 纹样设计数字指纹（SHA-256） */
    private String designHash;
    /** DST 打版文件指纹（SHA-256） */
    private String dstHash;
    private String stitchImageUrl;
    private String dstFileUrl;
    private Integer stitchCount;
    private String fileSize;
    /** 前端按 基础材料费 + 针数×单价 计算的动态报价 */
    private BigDecimal quoteAmount;
}
