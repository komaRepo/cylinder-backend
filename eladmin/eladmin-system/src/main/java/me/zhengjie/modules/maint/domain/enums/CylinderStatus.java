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
 * 气瓶状态枚举
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
public enum CylinderStatus {
    
    PRODUCED(0,"生产"),
    STOCK(1,"库存"),
    CIRCULATING(2,"流通"),
    REPAIR(3,"维修"),
    SCRAP(4,"报废");
    
    @EnumValue
    final int code;
    final String name;
    
    CylinderStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }
    
}
