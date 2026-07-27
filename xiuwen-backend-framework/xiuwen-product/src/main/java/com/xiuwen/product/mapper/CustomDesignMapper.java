package com.xiuwen.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuwen.product.entity.CustomDesign;
import com.xiuwen.product.entity.CustomDesignDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * custom_design 表 Mapper。
 */
@Mapper
public interface CustomDesignMapper extends BaseMapper<CustomDesign> {

    @Select("SELECT cd.*, " +
            "p.name AS productName, p.cover_image AS productCoverImage, " +
            "pt.title AS patternTitle, pt.image_url AS patternImageUrl " +
            "FROM custom_design cd " +
            "LEFT JOIN product p ON cd.product_id = p.id " +
            "LEFT JOIN pattern pt ON cd.pattern_id = pt.id " +
            "WHERE cd.id = #{designId} AND cd.deleted = 0")
    CustomDesignDetail selectDesignWithDetails(@Param("designId") Long designId);

}
