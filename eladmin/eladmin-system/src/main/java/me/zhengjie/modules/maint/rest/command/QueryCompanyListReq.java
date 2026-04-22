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
package me.zhengjie.modules.maint.rest.command;

import lombok.Data;
import me.zhengjie.modules.maint.domain.enums.CompanyStatus;
import me.zhengjie.modules.maint.domain.enums.CompanyType;

/**
 *
 * @author koma at cylinder-backend
 * @since 2026/3/25
 */
@Data
public class QueryCompanyListReq {
    
    /** 机构名称 */
    private String name;
    /** 机构类型 */
    private CompanyType type;
    /** 机构状态 */
    private CompanyStatus status;
    
    /** 页码 */
    private Integer page = 1;
    /** 每页大小 */
    private Integer size = 10;
    
}
