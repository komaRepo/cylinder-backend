package me.zhengjie.sys;

import lombok.Data;
import me.zhengjie.exception.BaseErrorInfo;

import java.io.Serializable;

/**
 * 全局统一响应体
 */
@Data
public class ResponseResult<T> implements Serializable {
    
    private Integer code;      // 状态码：200成功，500失败，401未登录等
    private String message;    // 提示信息
    private T data;            // 实际的数据载荷
    private long timestamp;    // 接口响应时间戳

    private ResponseResult() {
        this.timestamp = System.currentTimeMillis();
    }

    // 成功（无返回数据）
    public static <T> ResponseResult<T> success() {
        ResponseResult<T> responseResult = new ResponseResult<>();
        responseResult.setCode(200);
        responseResult.setMessage("操作成功");
        return responseResult;
    }

    // 成功（有返回数据）
    public static <T> ResponseResult<T> success(T data) {
        ResponseResult<T> responseResult = new ResponseResult<>();
        responseResult.setCode(200);
        responseResult.setMessage("操作成功");
        responseResult.setData(data);
        return responseResult;
    }

    // 失败（业务报错使用）
    public static <T> ResponseResult<T> error(Integer code, String message) {
        ResponseResult<T> responseResult = new ResponseResult<>();
        responseResult.setCode(code);
        responseResult.setMessage(message);
        return responseResult;
    }

    // 失败（默认500）
    public static <T> ResponseResult<T> error(String message) {
        return error(500, message);
    }
    
    
    public static <T> ResponseResult<T> error(BaseErrorInfo errorInfo) {
        ResponseResult<T> responseResult = new ResponseResult<>();
        responseResult.setCode(errorInfo.getCode());
        responseResult.setMessage(errorInfo.getMessage());
        return responseResult;
    }
    
}