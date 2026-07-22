package com.xiuwen.framework.web;

import com.xiuwen.common.constant.HttpStatus;
import com.xiuwen.common.core.domain.Result;
import com.xiuwen.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理。
 * 统一响应体格式，同时返回正确的 HTTP 状态码。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 处理主动抛出的业务异常 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        org.springframework.http.HttpStatus status = resolveHttpStatus(ex.getCode());
        return ResponseEntity.status(status)
                .body(Result.fail(ex.getCode(), ex.getMessage()));
    }

    /** 处理 @RequestBody 参数校验异常 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldError() == null
                ? "参数校验失败"
                : ex.getBindingResult().getFieldError().getDefaultMessage();
        return badRequest(message);
    }

    /** 处理普通表单参数绑定异常 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldError() == null
                ? "参数绑定失败"
                : ex.getBindingResult().getFieldError().getDefaultMessage();
        return badRequest(message);
    }

    /** 处理 @RequestParam 和 @PathVariable 参数校验异常 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        return badRequest(ex.getMessage());
    }

    /** 处理缺少请求参数异常 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        return badRequest("缺少必要参数：" + ex.getParameterName());
    }

    /** 处理 JSON 格式错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        return badRequest("请求 JSON 格式错误");
    }

    /** 处理不支持的请求方式 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex) {
        int code = org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED.value();
        return ResponseEntity.status(code)
                .body(Result.fail(code, "请求方式不支持：" + ex.getMethod()));
    }

    /** 处理未捕获的系统异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("系统异常", ex);
        return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(HttpStatus.ERROR, "系统异常，请稍后重试"));
    }

    private ResponseEntity<Result<Void>> badRequest(String message) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(Result.fail(HttpStatus.BAD_REQUEST, message));
    }

    private org.springframework.http.HttpStatus resolveHttpStatus(Integer code) {
        if (code == null) {
            return org.springframework.http.HttpStatus.BAD_REQUEST;
        }
        org.springframework.http.HttpStatus status =
                org.springframework.http.HttpStatus.resolve(code);
        return status == null
                ? org.springframework.http.HttpStatus.BAD_REQUEST
                : status;
    }
}
