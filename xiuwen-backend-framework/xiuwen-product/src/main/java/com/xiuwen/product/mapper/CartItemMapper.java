package com.xiuwen.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuwen.product.entity.CartItem;
import com.xiuwen.product.entity.CartItemDetail;
import com.xiuwen.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * cart_item 表 Mapper。
 */
@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
    @Select("SELECT ci.*, " +
            "p.name AS productName, p.cover_image AS productCoverImage, " +
            "p.price AS unitPrice, " +
            "cd.preview_image_url AS previewImageUrl " +
            "FROM cart_item ci " +
            "LEFT JOIN product p ON ci.product_id = p.id " +
            "LEFT JOIN custom_design cd ON ci.custom_design_id = cd.id " +
            "WHERE ci.user_id = #{userId} AND ci.deleted = 0 " +
            "ORDER BY ci.created_at DESC")
    List<CartItemDetail> selectCartItemsWithDetails(@Param("userId") Long userId);

    @Select("SELECT * FROM cart_item " +
            "WHERE user_id = #{userId} AND product_id = #{productId} " +
            "AND ((pattern_id IS NULL AND #{patternId} IS NULL) OR pattern_id = #{patternId}) " +
            "AND ((custom_design_id IS NULL AND #{customDesignId} IS NULL) OR custom_design_id = #{customDesignId}) " +
            "AND deleted = 0 LIMIT 1")
    CartItem selectExistingCartItem(@Param("userId") Long userId,
                                    @Param("productId") Long productId,
                                    @Param("patternId") Long patternId,
                                    @Param("customDesignId") Long customDesignId);


}
