package me.zhengjie.modules.maint.sys;

import lombok.Data;
import java.io.Serializable;

/**
 * 全局统一响应体
 */
@Data
public class Result<T> implements Serializable {
    
    private Integer code;      // 状态码：200成功，500失败，401未登录等
    private String message;    // 提示信息
    private T data;            // 实际的数据载荷
    private long timestamp;    // 接口响应时间戳

    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    // 成功（无返回数据）
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    // 成功（有返回数据）
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    // 失败（业务报错使用）
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    // 失败（默认500）
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
    
    
    public static <T> Result<T> error(BaseErrorInfo errorInfo) {
        Result<T> result = new Result<>();
        result.setCode(errorInfo.getCode());
        result.setMessage(errorInfo.getMessage());
        return result;
    }
    
}