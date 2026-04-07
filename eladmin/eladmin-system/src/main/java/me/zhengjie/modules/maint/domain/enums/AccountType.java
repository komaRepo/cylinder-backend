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
import lombok.Getter;

/**
 * 账号类型
 * @author koma at cylinder-backend
 * @since 2026/3/31
 */
@Getter
public enum AccountType {
    APP(1, "app"),
    ADMIN(2, "admin"),
    
    ;
    
    @EnumValue
    private final int code;
    private final String name;
    
    AccountType(int code, String name) {
        this.code = code;
        this.name = name;
    }
    
     public static AccountType getByCode(Integer code) {
        if (code == null) return null;
        for (AccountType type : AccountType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }
}
