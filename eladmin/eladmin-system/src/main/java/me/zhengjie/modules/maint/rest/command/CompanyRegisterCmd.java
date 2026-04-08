/*
 * Copyright 2026 The cylinder-backend Project under the WTFPL License,
 *
 *     http://www.wtfpl.net/about/
 *
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 * 代码千万行，注释第一行，编程不规范，日后泪两行
 *
 */
package me.zhengjie.modules.maint.rest.command;

import lombok.Data;
import me.zhengjie.modules.maint.domain.enums.CompanyType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 企业注册信息
 * @author koma at cylinder-backend
 * @since 2026/3/21
 */
@Data
public class CompanyRegisterCmd {

    @NotNull(message = "机构类型不能为空")
    private CompanyType type;
    
    @NotBlank(message = "机构名称不能为空")
    private String name;
    
    @NotBlank(message = "统一社会信用代码不能为空")
    private String creditCode;
    
    /** 监管部门分配代码 */
    private String code;
    
    @NotBlank(message = "法人姓名不能为空")
    private String legalName;
    
    @NotBlank(message = "法人证件号不能为空")
    private String legalCode;
    
    @NotBlank(message = "联系人电话不能为空")
    private String contactPhone;
    
    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;
    
    @NotBlank(message = "国家代码不能为空")
    private String countryCode;
    
    @NotBlank(message = "省份不能为空")
    private String province;
    
    private String city;
    
    @NotBlank(message = "区县不能为空")
    private String district;
    
    @NotBlank(message = "详细地址不能为空")
    private String address;
    
    /** 上级机构 */
    private Long parentId;
    
    /** 营业执照 */
    private String businessLicense;
    
    /** 危险化学品经营许可证(分销商必填) */
    private String dangerBusinessLicense;
    
    /** 气瓶充装许可证(加气站必填) */
    private String cylinderFillLicense;
    
    /** 特种设备使用许可证(加气站必填) */
    private String specialEquipmentLicense;
    
}
