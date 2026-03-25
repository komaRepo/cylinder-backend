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
 * TODO
 *
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
public enum EventType {
    PRODUCE(0,"生产"),
    SALE(1,"销售"),
    TRANSFER(2,"流转"),
    INFLATE(3,"充气"),
    INSPECTION(4,"年检"),
    REPAIR(4,"维修"),
    DISCARD(4,"报废"),
    ;
    
    @EnumValue
    final int code;
    final String name;
    
    EventType(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
