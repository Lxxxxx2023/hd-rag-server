package com.hd.rag.types.web;

import cn.hutool.core.util.StrUtil;
import com.hd.rag.types.convention.Result;
import com.hd.rag.types.errorcode.BaseErrorCode;
import com.hd.rag.types.exception.AbstractException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.BindingResult;

import java.util.Optional;

/**
 * 全局异常处理器
 * 拦截指定异常并通过优雅构建方式返回前端信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler{


    /**
     * 拦截参数验证异常
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result<Void> validExceptionHandler(HttpServletRequest request, MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        FieldError fieldError = bindingResult.getFieldError();
        String exceptionStr = Optional.ofNullable(fieldError)
                .map(FieldError::getDefaultMessage)
                .orElse(StrUtil.EMPTY);

        log.error("[{}] {} [ex] {}", request.getMethod(), getUrl(request), exceptionStr);
        return Results.failure(BaseErrorCode.CLIENT_ERROR.code(), exceptionStr);
    }

    /**
     * 拦截应用内抛出的异常
     */
    @ExceptionHandler(value = {AbstractException.class})
    public Result<Void> abstractException(HttpServletRequest request, AbstractException ex) {
        if(ex.getCause() != null) {
            log.error("[{}] {} [ex] {}", request.getMethod(), getUrl(request), ex, ex.getCause());
            return Results.failure(ex);
        }
        StringBuilder stackTraceBuilder = new StringBuilder();
        stackTraceBuilder.append(ex.getClass().getName()).append(": ").append(ex.getErrorMessage()).append("\n");
        StackTraceElement[] stackTrace = ex.getStackTrace();
        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
            stackTraceBuilder.append("\tat ").append(stackTrace[i]).append("\n");
        }
        log.error("[{}] {} [ex] {} \n\n{}", request.getMethod(), getUrl(request), ex, stackTraceBuilder);
        return Results.failure(ex);
    }

    /**
     * 拦截未捕获异常
     */
    @ExceptionHandler(value = Exception.class)
    public Result<Void> defaultErrorHandler(HttpServletRequest request, Exception throwable) {
        log.error("[{}] {} ", request.getMethod(), getUrl(request), throwable);
        return Results.failure();
    }

    private String getUrl(HttpServletRequest request) {
        if (StrUtil.isBlank(request.getQueryString())) {
            return request.getRequestURL().toString();
        }
        return request.getRequestURL().toString() + "?" + request.getQueryString();
    }
}
