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
 * 机构类型
 * @author koma at cylinder-backend
 * @since 2026/3/21
 */
public enum CompanyType {
    MANUFACTURER(0,"制造商"),
    DISTRIBUTOR(1,"分销商"),
    RETAILER(2,"加气站"),
    INSPECTION(3,"年检机构"),
    ;
    
    @EnumValue
    final int code;
    final String name;
    
    CompanyType(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
