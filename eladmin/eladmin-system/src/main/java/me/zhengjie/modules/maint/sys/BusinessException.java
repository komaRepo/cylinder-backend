package me.zhengjie.modules.maint.sys;

import lombok.Getter;

/**
 * 自定义全局业务异常
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private Integer code;

    // 保留原有的字符串构造（兼容一些动态拼接的报错，比如 "气瓶[A123]不存在"）
    public BusinessException(String message) {
        super(message);
        this.code = 500; 
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    // 【新增核心构造器】：直接接收枚举
    public BusinessException(BaseErrorInfo errorInfo) {
        super(errorInfo.getMessage());
        this.code = errorInfo.getCode();
    }
}