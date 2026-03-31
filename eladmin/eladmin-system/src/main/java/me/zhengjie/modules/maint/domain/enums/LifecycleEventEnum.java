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
import lombok.Getter;

/**
 * TODO
 *
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
/**
 * 生命周期事件枚举 (完美对应 CylinderLifecycle 表的 eventType)
 */
@Getter
public enum LifecycleEventEnum {
    PRODUCE(1, "生产建档"),
    SALE(2, "销售终端"),
    TRANSFER_OUT(3, "扫码出库"), // 细化流转动作
    TRANSFER_IN(8, "扫码入库"),  // 细化流转动作 (新增个8，或者你统称3)
    FILL(4, "扫码充气"),
    INSPECT(5, "年检验收"),
    REPAIR(6, "送修"),
    SCRAP(7, "强制报废");
    
    @EnumValue
    private final int code;
    private final String name;
    
    LifecycleEventEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }
    
    
    public static LifecycleEventEnum getEventEnumByCode(Integer code) {
        if (code == null) return null;
        for (LifecycleEventEnum e : LifecycleEventEnum.values()) {
            if (e.getCode() == code) {
                return e;
            }
        }
        return null;
    }
    
}
