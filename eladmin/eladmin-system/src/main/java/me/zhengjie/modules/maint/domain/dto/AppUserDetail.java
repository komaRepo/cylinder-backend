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
package me.zhengjie.modules.maint.domain.dto;

import lombok.Data;
import me.zhengjie.modules.maint.domain.enums.UserStatus;
import me.zhengjie.modules.maint.domain.enums.UserType;

import java.util.Date;

/**
 * app账号用户信息
 * @author koma at cylinder-backend
 * @since 2026/3/23
 */
@Data
public class AppUserDetail {
    
    private Long id;
    
    private String username;
    
    private String password;
    
    private String phone;
    
    private Long companyId;
    
    private UserType userType;
    
    private UserStatus status;
    
    private Long activatorId;
    
    private Date lastLogin;
    
    private Date createTime;
    
}
