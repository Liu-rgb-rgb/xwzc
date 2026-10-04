package com.xiuwen.web.controller.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiuwen.common.core.domain.PageResult;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import com.xiuwen.system.dto.UserStatusRequest;
import com.xiuwen.system.entity.User;
import com.xiuwen.system.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
public class MerchantUserController {

    private final UserService userService;

    public MerchantUserController(UserService userService) {
        this.userService = userService;
    }

    /** [11.1] 用户列表 */
    @GetMapping
    public Result<PageResult<User>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            query.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getEmail, keyword));
        }
        if (role != null && !role.isEmpty()) {
            query.eq(User::getRole, role);
        }
        if (status != null) {
            query.eq(User::getStatus, status);
        }
        query.orderByDesc(User::getCreatedAt);
        Page<User> pageParam = new Page<>(page, pageSize);
        Page<User> result = userService.page(pageParam, query);
        return Result.success(PageResult.of(
                result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    /** [11.2] 用户详情 */
    @GetMapping("/{userId}")
    public Result<User> detail(@PathVariable Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return Result.success(user);
    }

    /** [11.3] 禁用或恢复用户 */
    @PutMapping("/{userId}/status")
    public Result<Void> toggleStatus(@PathVariable Long userId, @Valid @RequestBody UserStatusRequest request) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        User update = new User();
        update.setId(userId);
        update.setStatus(request.getStatus());
        userService.updateById(update);
        return Result.success();
    }
}
