package com.xiuwen.web.controller.merchant;

import lombok.Data;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.xiuwen.common.core.domain.Result;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * ClassName: MerchantMessageController
 * Package: com.xiuwen.web.controller.merchant
 * Description:
 *
 * @Author jacksonling
 * @Create 2026/7/13 23:12
 * @Version 1.0
 */
@Data
@RequestMapping("/api/admin/messages")
public class MerchantMessageController {
    public Result<Void> message(){return Result.todo("管理员发布信息");}
}

