package me.zhengjie.modules.maint.domain.enums;

import lombok.Getter;

/**
 * 扫码类型（scan_record.scan_type）统一口径定义。
 */
@Getter
public enum ScanType {
    QUERY(1, "查询"),
    OUTBOUND(2, "出库"),
    INBOUND(3, "入库"),
    FILL(4, "充装"),
    INSPECTION(5, "年检"),
    REPAIR(6, "维修"),
    SCRAP(7, "报废"),
    ;

    private final int code;
    private final String name;

    ScanType(int code, String name) {
        this.code = code;
        this.name = name;
    }
}
