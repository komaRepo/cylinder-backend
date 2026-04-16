package me.zhengjie.modules.maint.domain.enums;

import lombok.Getter;

/**
 * APP 端操作权限标识枚举
 */
@Getter
public enum AppPermissionCode {
    
    IN("app:cylinder:in", "入库"),
    OUT("app:cylinder:out", "出库"),
    FILL("app:cylinder:fill", "充气"),
    INSPECT("app:cylinder:inspect", "年检"),
    SCRAP("app:cylinder:scrap", "报废"),
    REPAIR("app:cylinder:repair", "维修");

    private final String code;
    private final String desc;

    AppPermissionCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}