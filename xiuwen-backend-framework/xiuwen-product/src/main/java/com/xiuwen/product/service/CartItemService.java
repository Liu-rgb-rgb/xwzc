package com.xiuwen.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiuwen.product.entity.CartItem;
import com.xiuwen.product.entity.CartItemDetail;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * cart_item 表服务接口。
 */
public interface CartItemService extends IService<CartItem> {
    CartItemDetail addToCart(Long userId, Long productId, Long patternId,
                             Long customDesignId, Integer quantity);

    List<CartItemDetail> listCartItems(Long userId);

    void clearCart(Long userId);

    CartItemDetail updateCart(Long userId, Long cartItemId, Integer quantity, Integer selected);

    void deletedCart(Long userId, Long cartItemId);

    CartItemDetail getCartItemDetail(Long cartItemId);
}
