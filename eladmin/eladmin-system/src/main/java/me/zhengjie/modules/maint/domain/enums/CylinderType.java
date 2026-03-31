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

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 气瓶种类枚举
 * @author koma at cylinder-backend
 * @since 2026/3/31
 */
public enum CylinderType {
    SEAMLESS(1,"无缝气瓶"),
    COMPOSITE(1,"复合瓶"),
    ;
    
    @EnumValue
    final int code;
    final String name;
    
    CylinderType(int code, String name) {
        this.code = code;
        this.name = name;
    }
    
    
    public static CylinderType of(int code) {
        for (CylinderType type : CylinderType.values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid CylinderType code: " + code);
    }
    
}
