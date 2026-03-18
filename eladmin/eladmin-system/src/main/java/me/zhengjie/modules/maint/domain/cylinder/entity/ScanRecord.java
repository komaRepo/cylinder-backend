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
package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * TODO
 *
 * @author koma at cylinder-backend
 * @since 2026/3/18
 */
@Data
@TableName("scan_record")
@Schema(description = "扫码记录表")
public class ScanRecord {
    @TableId(type = IdType.AUTO)
    @Schema(description = "扫码记录ID")
    private Long id;
    
    @Schema(description = "气瓶ID")
    private Long cylinderId;
    
    @Schema(description = "扫码用户ID")
    private Long userId;
    
    @Schema(description = "扫码企业ID")
    private Long companyId;
    
    @Schema(description = "扫码类型 1查询 2充气 3年检 4入库 5出库")
    private Integer scanType;
    
    @Schema(description = "扫码时间(分区键)")
    @TableField(fill = FieldFill.INSERT)
    private Date scanTime;
}
