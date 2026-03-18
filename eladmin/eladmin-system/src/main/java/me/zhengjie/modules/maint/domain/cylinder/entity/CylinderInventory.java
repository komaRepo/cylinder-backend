package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@TableName("cylinder_inventory")
@Schema(description = "气瓶库存表")
public class CylinderInventory {
    @TableId(type = IdType.AUTO)
    @Schema(description = "库存ID")
    private Long id;
    
    @Schema(description = "企业ID")
    private Long companyId;
    
    @Schema(description = "气瓶ID")
    private Long cylinderId;
    
    @Schema(description = "库存状态 1正常 2维修 3待检")
    private Integer status;
    
    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}