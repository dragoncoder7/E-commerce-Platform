package com.ecommerce.store.config;

import com.ecommerce.common.core.domain.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 注册唯一性冲突由数据库兜底，不暴露数据库字段及语句。 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StoreExceptionHandler {
  @ExceptionHandler(DuplicateKeyException.class)
  public Result<Void> duplicate() {
    return Result.fail(409, "账号、手机号或邮箱已被注册");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public Result<Void> invalidRequest() {
    return Result.fail(400, "请求格式不正确");
  }
}
