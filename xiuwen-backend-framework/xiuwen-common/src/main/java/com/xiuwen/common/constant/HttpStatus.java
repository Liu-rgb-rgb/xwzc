package com.xiuwen.common.constant;

/**
 * 业务返回码常量。
 * 说明：这里是响应体中的 code，不等同于 HTTP 状态码。
 */
public interface HttpStatus {
    Integer SUCCESS = 200;
    Integer BAD_REQUEST = 400;
    Integer UNAUTHORIZED = 401;
    Integer FORBIDDEN = 403;
    Integer NOT_FOUND = 404;
    Integer ERROR = 500;
}
