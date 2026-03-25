/*
 * Copyright 2026 The rfid-backend Project under the WTFPL License,
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
package me.zhengjie.modules.maint.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 企业状态
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
public enum CompanyStatus {
    ACTIVE(0,"待激活"),
    INACTIVE(1,"正常"),
    SUSPENDED(2,"禁用"),
    DELETED(3,"删除");
    
    @EnumValue
    final int code;
    final String name;
    
    CompanyStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
