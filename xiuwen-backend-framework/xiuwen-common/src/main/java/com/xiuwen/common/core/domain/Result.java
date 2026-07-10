package com.xiuwen.common.core.domain;

import com.xiuwen.common.constant.HttpStatus;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一接口返回对象。
 *
 * @param <T> data 数据类型
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 业务状态码：200 成功，400 参数错误，401 未登录，403 无权限，500 系统异常 */
    private Integer code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return build(HttpStatus.SUCCESS, "success", data);
    }

    public static <T> Result<T> fail(String message) {
        return build(HttpStatus.ERROR, message, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return build(code, message, null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return build(HttpStatus.UNAUTHORIZED, message, null);
    }

    public static <T> Result<T> forbidden(String message) {
        return build(HttpStatus.FORBIDDEN, message, null);
    }

    public static <T> Result<T> todo(String moduleName) {
        return build(HttpStatus.SUCCESS, moduleName + " 接口已预留，请补充业务实现", null);
    }

    private static <T> Result<T> build(Integer code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}
