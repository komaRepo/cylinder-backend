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
 * 气瓶地区分布快照表实体类
 * @author koma at cylinder-backend
 * @since 2026/3/18
 */
@Data
@TableName("cylinder_distribution_stats")
@Schema(description = "气瓶地区分布快照表")
public class CylinderDistributionStats {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "省")
    private String province;
    
    @Schema(description = "市")
    private String city;
    
    @Schema(description = "所属企业ID")
    private Long companyId;
    
    @Schema(description = "气瓶规格")
    private String cylinderSpec;
    
    @Schema(description = "气瓶总数")
    private Integer totalCount;
    
    @Schema(description = "快照更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
