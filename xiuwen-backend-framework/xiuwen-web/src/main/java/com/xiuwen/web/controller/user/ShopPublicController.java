package com.xiuwen.web.controller.user;


import com.xiuwen.common.core.domain.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * ClassName: ShopPublicController
 * Package: com.xiuwen.web.controller.user
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/13 22:27
 * @Version 1.0
 */

@RestController
@RequestMapping("/api/shop")
public class ShopPublicController {

    @GetMapping("/info")
    public Result<Void> info() {
        return Result.todo("前台店铺信息");
    }
}
