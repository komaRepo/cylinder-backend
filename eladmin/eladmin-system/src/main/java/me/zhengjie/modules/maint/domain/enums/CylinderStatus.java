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
 * 气瓶状态枚举
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
/**
 * 气瓶当前物理状态枚举 (对应 cylinder 主表的 status)
 */
@Getter
public enum CylinderStatus {
    PRODUCED(0, "已建档"),   // 刚贴签，还没正式入库
    IN_STOCK(1, "在库"),     // 静静地躺在仓库里
    TRANSIT(2, "运输/流转中"),// 出库了，但对方还没扫码接收
    WAIT_INSPECT(3, "待检/维修"), // 过期或损坏，被强制锁定
    SCRAP(4, "已报废");      // 彻底死亡，不可再用
    
    @EnumValue
    private final int code;
    private final String name;
    
    CylinderStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
