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
public enum OperationType {
    CREATE(0,"出库"),
    STORAGE(1,"入库"),
    INFLATE(2,"加气"),
    INSPECTION(3,"年检"),
    ;
    
    private final int code;
    private final String name;
    
    OperationType(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
