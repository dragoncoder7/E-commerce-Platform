package com.ecommerce.system.config;

import com.ecommerce.common.core.domain.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 将请求错误与真正的服务故障区分，避免缺失接口被报告成系统繁忙。 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    public Result<Void> duplicate(org.springframework.dao.DuplicateKeyException exception) {
        return Result.fail(409, "编码或数据已存在，请检查后重试");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> notFound() {
        return Result.fail(404, "请求的接口不存在");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> methodNotAllowed() {
        return Result.fail(405, "请求方法不支持");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> invalidRequest() {
        return Result.fail(400, "请求参数格式不正确");
    }
}
