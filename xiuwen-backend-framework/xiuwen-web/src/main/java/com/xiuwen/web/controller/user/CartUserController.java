package com.xiuwen.web.controller.user;

import com.xiuwen.common.core.domain.Result;

import com.xiuwen.framework.security.LoginUserHolder;
import com.xiuwen.product.dto.CartAddDTO;
import com.xiuwen.product.dto.CartUpdateDTO;
import com.xiuwen.product.entity.CartItem;
import com.xiuwen.product.entity.CartItemDetail;
import com.xiuwen.product.service.CartItemService;
import com.xiuwen.product.vo.CartItemVO;
import com.xiuwen.product.vo.CartListVO;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Delete;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

/**
 * 用户端购物车接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/cart")
public class CartUserController {

    private final CartItemService cartItemService;



    //购物车列表
    @GetMapping
    public Result<CartListVO> list() {
        Long userId = LoginUserHolder.getUserId();
        List<CartItemDetail> items = cartItemService.listCartItems(userId);
        List<CartItemVO> voList = items.stream().map(this::toCartItemVO).toList();
        CartListVO cartListVO = new CartListVO();
        cartListVO.setItems(voList);
        cartListVO.setTotalQuantity(voList.stream()
                .mapToInt(CartItemVO::getQuantity).sum());
        cartListVO.setTotalAmount(voList.stream()
                .filter(item -> item.getSelected() != null && item.getSelected() == 1)
                .map(CartItemVO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return Result.success(cartListVO);
    }
//加入购物车
    @PostMapping
    public Result<CartItemVO> addCart(@Valid @RequestBody CartAddDTO cartAddDTO) {
        Long userId = LoginUserHolder.getRequiredUserId();
        CartItemDetail cartItem = cartItemService.addToCart(
                userId,
                cartAddDTO.getProductId(),
                cartAddDTO.getPatternId(),
                cartAddDTO.getCustomDesignId(),
                cartAddDTO.getQuantity()
        );

        return Result.success(toCartItemVO(cartItem));
    }
//清空购物车
    @DeleteMapping
    public Result<Void> clearCart() {
        Long userId = LoginUserHolder.getRequiredUserId();
        cartItemService.clearCart(userId);
        return Result.success();
    }
//修改购物车
    @PutMapping("/{cartItemId}")
    public Result<CartItemVO> updateCart(
            @PathVariable Long cartItemId,
            @RequestBody CartUpdateDTO cartUpdateDTO
    ) {
        Long userId = LoginUserHolder.getRequiredUserId();
     CartItemDetail cartItem =   cartItemService.updateCart(
                userId,
                cartItemId,
                cartUpdateDTO.getQuantity(),
                cartUpdateDTO.getSelected()
        );
        return Result.success(toCartItemVO(cartItem));
    }
//删除购物车
    @DeleteMapping("{cartItemId}")
    public Result<Void> deleted(@PathVariable Long cartItemId) {
        Long userId = LoginUserHolder.getRequiredUserId();
        cartItemService.deletedCart(userId,cartItemId);
        return Result.success();
    }



    private CartItemVO toCartItemVO(CartItemDetail detail) {
        CartItemVO vo = CartItemVO.from(detail);
        vo.setProductName(detail.getProductName());
        vo.setProductCoverImage(detail.getProductCoverImage());
        vo.setUnitPrice(detail.getUnitPrice());
        vo.setPreviewImageUrl(detail.getPreviewImageUrl());

        // 计算小计
        if (detail.getUnitPrice() != null && detail.getQuantity() != null) {
            vo.setSubtotal(detail.getUnitPrice()
                    .multiply(BigDecimal.valueOf(detail.getQuantity())));
        }

        return vo;
    }
}
