package com.xiuwen.pattern.vo;

import com.xiuwen.pattern.entity.PromptTemplate;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提示词模板响应对象
 */
@Data
public class PromptTemplateVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String style;
    private String usageScene;
    private String colorTheme;
    private String templateText;
    private Integer status;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PromptTemplateVO from(PromptTemplate entity) {
        if (entity == null) return null;
        PromptTemplateVO vo = new PromptTemplateVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setStyle(entity.getStyle());
        vo.setUsageScene(entity.getUsageScene());
        vo.setColorTheme(entity.getColorTheme());
        vo.setTemplateText(entity.getTemplateText());
        vo.setStatus(entity.getStatus());
        vo.setSort(entity.getSort());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
