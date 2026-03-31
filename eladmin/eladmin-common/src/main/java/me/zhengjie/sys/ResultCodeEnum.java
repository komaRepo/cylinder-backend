package me.zhengjie.sys;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.zhengjie.exception.BaseErrorInfo;

/**
 * 全局业务错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCodeEnum implements BaseErrorInfo {

    // ========== 通用基础状态码 ==========
    SUCCESS(200, "操作成功"),
    FAILED(500, "系统开小差了，请稍后再试"),
    UNAUTHORIZED(401, "登录已过期，请重新登录"),
    FORBIDDEN(403, "抱歉，您没有相关权限"),
    PARAM_ERROR(400, "请求参数校验失败"),
    

    // ========== 用户与权限业务异常 (1000段) ==========
    USER_NOT_EXIST(1001, "该用户不存在"),
    PASSWORD_ERROR(1002, "账号或密码错误"),
    ACCOUNT_PENDING(1003, "您的账号正在等待企业管理员审核激活"),
    ACCOUNT_DISABLED(1004, "您的账号已被禁用，请联系管理员"),
    COMPANY_NOT_EXIST(1005, "您所选择的企业不存在或已被禁用"),
    COMPANY_NOT_BIND(1006, "您的账号暂未绑定企业，无法执行相关操作"),
    LEAPFROG_OPERATION(1007, "越权操作！您只能将账号绑定到【本企业】或【直接下级企业】名下"),
    ACCOUNT_STATUS_ERROR(1008, "您当前的账号为非正常状态，无法执行该操作"),
    PASSWORD_VERIFY_ERROR(1009, "新密码不能与旧密码相同"),

    // ========== 气瓶与流转业务异常 (2000段) ==========
    CYLINDER_NOT_FOUND(2001, "未查询到该气瓶信息"),
    CYLINDER_STATUS_ERROR(2002, "当前气瓶状态异常，无法执行此操作"),
    NO_FILLING_PERMISSION(2003, "您所在的企业没有充气资质"),
    FLOW_NOT_MATCH(2004, "气瓶当前不在您所在企业名下，无法出库"),
    QRCODE_INVALID(2005, "无效的气瓶二维码");

    private final Integer code;
    private final String message;
}