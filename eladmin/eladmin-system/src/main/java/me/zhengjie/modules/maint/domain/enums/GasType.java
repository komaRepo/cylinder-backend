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
package me.zhengjie.modules.maint.domain.enums;

/**
 * 气体类型
 * @author koma at cylinder-backend
 * @since 2026/3/27
 */
public enum GasType {
    OXYGEN(1, "氧气"),
    NITROGEN(2, "氮气"),
    ARGON(3, "氩气"),
    CARBON_DIOXIDE(4, "二氧化碳"),
    ACETYLENE(5, "乙炔"),
    HYDROGEN(6, "氢气"),
    CNG(7, "天然气"),
    OTHER(99, "其他"),
    ;
    
    private final int code;
    private final String description;
    
    GasType(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}
