package com.xiuwen.common.exception;

import com.xiuwen.common.constant.HttpStatus;

/**
 * 业务异常。
 * 用于主动抛出可预期的业务错误，例如：账号不存在、密码错误、订单状态不允许流转。
 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
