package me.zhengjie.modules.maint.sys;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常拦截器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 拦截我们自己抛出的业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 兜底拦截未知的系统运行时异常 (比如空指针、数据库连不上)
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统内部异常: ", e);
        // 生产环境可以把 e.getMessage() 换成 "系统繁忙，请稍后再试"，防止暴露系统底层错误
        return Result.error(500, "系统开小差了: " + e.getMessage());
    }
}