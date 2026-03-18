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

/**
 * TODO
 *
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
public enum TargetType {
    USER(0,"用户"),
    COMPANY(1,"公司"),
    PERMISSION(2,"权限"),
    ;
    
    private final int code;
    private final String name;
    
    TargetType(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
