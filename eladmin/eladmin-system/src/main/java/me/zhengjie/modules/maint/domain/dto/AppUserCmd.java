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

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import me.zhengjie.modules.maint.domain.enums.UserStatus;

import java.util.Date;

/**
 *
 * @author koma at cylinder-backend
 * @since 2026/3/23
 */
@Data
public class AppUserCmd {
    
    /** 用户状态 */
    private UserStatus status;
    /** 用户名 */
    private String username;
    /** 用户手机号 */
    private String phone;
    /** 用户创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Date createTimeStart;
    /** 用户创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Date createTimeEnd;
    
}
