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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 分页查询
 * @author koma at cylinder-backend
 * @since 2026/3/30
 */
@Data
public class PageQueryReq {
    
    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码必须大于或等于1")
    private Integer page = 1;
    
    @Schema(description = "每页条数", example = "15")
    @Max(value = 100, message = "每页条数必须小于或等于100")
    private Integer size = 15;
    
}
