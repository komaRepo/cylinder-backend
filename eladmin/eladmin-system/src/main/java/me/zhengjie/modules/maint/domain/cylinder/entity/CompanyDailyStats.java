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
 *
 * @author koma at cylinder-backend
 * @since 2026/3/18
 */
@Data
@TableName("company_daily_stats")
@Schema(description = "企业每日业务汇总表")
public class CompanyDailyStats {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "企业ID")
    private Long companyId;
    
    @Schema(description = "统计日期")
    private Date statDate;
    
    @Schema(description = "当日充气总数")
    private Integer fillCount;
    
    @Schema(description = "当日年检总数")
    private Integer inspectionCount;
    
    @Schema(description = "当日入库总数")
    private Integer inCount;
    
    @Schema(description = "当日出库总数")
    private Integer outCount;
    
    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
