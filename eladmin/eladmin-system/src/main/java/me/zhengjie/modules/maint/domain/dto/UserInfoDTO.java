package me.zhengjie.modules.maint.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

/**
 * 登录用户上下文信息 DTO
 * 包含了用户基本信息及其所属企业的核心属性，用于缓存和 Token 载荷
 */
@Data
@Schema(description = "登录用户上下文信息")
public class UserInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ================== 用户基本信息 ==================
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "用户类型 1企业管理员 2普通员工")
    private Integer userType;

    @Schema(description = "账号状态 0待激活 1正常 2禁用")
    private Integer status;


    // ================== 所属企业核心上下文 ==================
    @Schema(description = "所属企业ID")
    private Long companyId;

    @Schema(description = "企业名称")
    private String companyName;

    // 企业的层级路径（极其重要！用于树形结构向下穿透查询极速过滤）
    // 比如：当前登录用户是省级代理(ID:5)，path是 "0,1,5,"
    // 那么他查气瓶时，SQL 直接拼接: WHERE owner_path LIKE '0,1,5,%'
    @Schema(description = "企业层级路径")
    private String companyPath;


    // ================== 企业身份权限标识 ==================
    // 为什么把企业的 type 也放进用户的上下文里？
    // 因为这决定了该用户在 APP 上的菜单显示和功能权限！
    
    @Schema(description = "是否拥有制造商权限 0否 1是")
    private Integer typeManufacturer;

    @Schema(description = "是否拥有经销商权限(出入库) 0否 1是")
    private Integer typeDealer;

    @Schema(description = "是否拥有充气商权限(充气功能) 0否 1是")
    private Integer typeFiller;

    @Schema(description = "是否拥有年检机构权限(年检功能) 0否 1是")
    private Integer typeInspection;
}