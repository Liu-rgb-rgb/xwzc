package com.xiuwen.framework.security;

import com.xiuwen.common.constant.HttpStatus;
import com.xiuwen.common.constant.RoleConstants;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.framework.web.ResponseWriter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 鉴权拦截器。
 *
 * 负责：
 * 1. 校验需要登录的接口是否携带有效 Token。
 * 2. 校验 /api/merchant/** 是否为 MERCHANT_ADMIN 或 ADMIN。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {
    private final JwtUtils jwtUtils;

    public JwtAuthInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null) {
            ResponseWriter.writeJson(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    Result.fail(
                            HttpStatus.UNAUTHORIZED,
                            "未登录或登录已过期"
                    )
            );
            return false;
        }

        LoginUser loginUser;
        try {
            loginUser = jwtUtils.parseToken(token);
        } catch (Exception ex) {
            ResponseWriter.writeJson(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    Result.fail(
                            HttpStatus.UNAUTHORIZED,
                            "Token 无效或已过期"
                    )
            );
            return false;
        }

        if (isMerchantApi(request)
                && !isMerchantOrAdmin(loginUser)) {
            ResponseWriter.writeJson(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    Result.fail(
                            HttpStatus.FORBIDDEN,
                            "无权限访问商家后台接口"
                    )
            );
            return false;
        }

        LoginUserHolder.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        LoginUserHolder.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null
                && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            return token.isEmpty() ? null : token;
        }

        String token = request.getHeader("token");
        if (token != null && !token.trim().isEmpty()) {
            return token.trim();
        }

        token = request.getParameter("token");
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        return token.trim();
    }

    private boolean isMerchantApi(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        if (contextPath == null) {
            contextPath = "";
        }

        String uri = request.getRequestURI();
        String path = uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;

        return path.equals("/api/merchant")
                || path.startsWith("/api/merchant/")
                || path.equals("/api/admin")
                || path.startsWith("/api/admin/");
    }

    private boolean isMerchantOrAdmin(LoginUser loginUser) {
        if (loginUser == null || loginUser.getRole() == null) {
            return false;
        }

        String role = loginUser.getRole();
        return RoleConstants.MERCHANT_ADMIN.equals(role)
                || RoleConstants.ADMIN.equals(role);
    }
}