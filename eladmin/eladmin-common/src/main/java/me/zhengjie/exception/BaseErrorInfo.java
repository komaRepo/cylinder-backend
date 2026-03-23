package me.zhengjie.exception;

/**
 * 基础错误信息接口
 */
public interface BaseErrorInfo {
    /** 错误码 */
    Integer getCode();
    /** 错误描述 */
    String getMessage();
}