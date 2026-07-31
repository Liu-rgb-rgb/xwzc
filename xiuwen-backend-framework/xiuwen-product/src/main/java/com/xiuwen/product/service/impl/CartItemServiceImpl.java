package com.xiuwen.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.product.entity.CartItem;
import com.xiuwen.product.entity.CartItemDetail;
import com.xiuwen.product.entity.Product;
import com.xiuwen.product.mapper.CartItemMapper;
import com.xiuwen.product.service.CartItemService;
import com.xiuwen.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * cart_item 表服务实现。
 */
@RequiredArgsConstructor
@Service
public class CartItemServiceImpl extends ServiceImpl<CartItemMapper, CartItem> implements CartItemService {
   private final ProductService productService;


//购物车列表
    @Override
    public List<CartItemDetail> listCartItems(Long userId) {
        return baseMapper.selectCartItemsWithDetails(userId);
    }
//清空购物车
    @Override
    public void clearCart(Long userId) {
     List<CartItemDetail> item = baseMapper.selectCartItemsWithDetails(userId);
     removeByIds(item.stream().map(CartItemDetail::getId).toList());
    }
//修改购物车
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartItemDetail updateCart(Long userId, Long cartItemId, Integer quantity, Integer selected) {
     CartItem cartItem = getById(cartItemId);
     if(cartItem==null || !cartItem.getUserId().equals(userId)){
         throw new BusinessException("购物车不存在,无权操作");
     }
     if(quantity != null){
         if(quantity <= 0){
             throw new BusinessException("商品数量必须大于0");
         }
         cartItem.setQuantity(quantity);
     }
     if(selected != null){
         cartItem.setSelected(selected);
     }
     return baseMapper.selectCartItemsWithDetails(userId).stream()
             .filter(item -> item.getId().equals(cartItem.getId()))
             .findFirst().orElse(null);
    }

    @Override
    public void deletedCart(Long userId, Long cartItemId) {
        CartItem cartItem = getById(cartItemId);
        if(cartItem==null || !cartItem.getUserId().equals(userId)){
            throw new BusinessException("无权操作,或购物车不存在");
        }
        removeById(cartItemId);
    }

    //添加购物车
    @Transactional(rollbackFor = Exception.class)
    @Override
    public CartItemDetail addToCart(Long userId, Long productId, Long patternId, Long customDesignId, Integer quantity) {
        Product product = productService.getById(productId);
        if(product == null || !"ON_SALE".equals(product.getStatus())){
            throw new BusinessException("商品不存在或已下架");
        }
        if(quantity == null || quantity <= 0){
            quantity = 1;
        }
        CartItem existing = baseMapper.selectExistingCartItem(userId,productId,patternId,customDesignId);
        if(existing != null){
            existing.setQuantity(existing.getQuantity() + quantity);
            baseMapper.updateById(existing);
            return baseMapper.selectCartItemsWithDetails(userId).stream()
                    .filter(item -> item.getId().equals(existing.getId()))
                    .findFirst().orElse(null);
        }
        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setProductId(productId);
        cartItem.setPatternId(patternId);
        cartItem.setCustomDesignId(customDesignId);
        cartItem.setQuantity(quantity);
        cartItem.setSelected(1);
        save(cartItem);

        // 返回带详情的购物车项
        return baseMapper.selectCartItemsWithDetails(userId).stream()
                .filter(item -> item.getId().equals(cartItem.getId()))
                .findFirst().orElse(null);


    }
    /**
     * 根据购物车ID获取单个详情
     * 用于下单时校验和获取价格信息
     */
    @Override
    public CartItemDetail getCartItemDetail(Long cartItemId) {
        // 1. 使用 MP 自带方法查询购物车基础信息
        CartItem cartItem = this.getById(cartItemId);
        if (cartItem == null) {throw new BusinessException("购物车项不存在");}
        // 2. 手动构建 Detail 对象并赋值
        CartItemDetail detail = new CartItemDetail();
        // 复制基础属性 (id, userId, productId, quantity 等)
        BeanUtils.copyProperties(cartItem, detail);
        // 3. 根据 productId 查询商品信息 (利用已有的 productService)
        Product product = productService.getById(cartItem.getProductId());
        if (product != null) {
            // 将商品信息填充到 Detail 中
            detail.setProductName(product.getName());
            detail.setProductCoverImage(product.getCoverImage());
            detail.setUnitPrice(product.getPrice()); // 关键：获取价格
            detail.setStock(product.getStock());
        }
        return detail;
    }
}
