package com.xiuwen.framework.security;

import com.xiuwen.common.constant.HttpStatus;
import com.xiuwen.common.exception.BusinessException;

/**
 * 当前线程登录用户上下文。
 * Controller / Service 中可通过该类获取当前登录用户 ID 和 role。
 */
public final class LoginUserHolder {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginUserHolder() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser getRequired() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }
        return user;
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static Long getRequiredUserId() {
        return getRequired().getUserId();
    }

    public static String getRole() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
