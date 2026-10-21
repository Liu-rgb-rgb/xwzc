package com.xiuwen.pattern.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * production_order 表实体：工厂端生产工单。
 *
 * 客户端在 AI 打版完成后"提交给工厂"生成一条工单，
 * 工厂/绣娘端据此排产并下载工业级 DST 文件。
 */
@Data
@TableName("production_order")
public class ProductionOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    private Integer deleted;

    /** 工单号，如 PO20261015153012-47 */
    private String orderNo;

    private Long userId;
    private String userNickname;

    /** 接单时写入的承接工坊名称，如：岭南乡村非遗工坊 */
    private String factoryName;

    private Long patternId;
    private String patternTitle;
    private String patternImageUrl;
    private String style;

    /** 针法：OUTLINE 轮廓走针 / SATIN 平包针 / TATAMI 他他米针 */
    private String stitchType;

    /** 面料类型：canvas 帆布 / cotton 棉布 / silk 丝绸（打版张力补偿参数） */
    private String fabricType;

    /** 纹样设计数字指纹（SHA-256），版权存证与防伪造溯源 */
    private String designHash;

    /** DST 打版文件指纹（SHA-256） */
    private String dstHash;

    /** 针脚路径图（打版可视化图） */
    private String stitchImageUrl;

    /** 工业级 DST 文件下载地址 */
    private String dstFileUrl;

    /** 针数 */
    private Integer stitchCount;

    /** 文件大小，如 118 KB */
    private String fileSize;

    /** 动态报价：基础材料费 + 针数 × 单价 */
    private BigDecimal quoteAmount;

    /** 工单状态：PENDING 待生产 / IN_PRODUCTION 生产中 / COMPLETED 已完成 */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
