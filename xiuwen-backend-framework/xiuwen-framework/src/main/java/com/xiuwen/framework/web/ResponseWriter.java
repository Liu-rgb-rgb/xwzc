package com.xiuwen.framework.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiuwen.common.core.domain.Result;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 直接向 HttpServletResponse 写 JSON。
 * 主要用于拦截器中返回 401 / 403。
 */
public final class ResponseWriter {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ResponseWriter() {
    }

    public static void writeJson(HttpServletResponse response, int httpStatus, Result<?> result) throws IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(result));
    }
}
