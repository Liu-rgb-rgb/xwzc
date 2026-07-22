package com.xiuwen.pattern.vo;

/**
 * ClassName: PatternItemVO
 * Package: com.xiuwen.pattern.vo
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/11 10:39
 * @Version 1.0
 */


import lombok.Data;
import java.time.LocalDateTime;
@Data
/**
 * ai生成的单张纹样结果
 */
public class PatternItemVO {
    private Long id;
    private String title;
    private String imageUrl;
    private String thumbnailUrl;
    private String style;
    private Boolean isFavorite;
    private LocalDateTime createdAt;
}
