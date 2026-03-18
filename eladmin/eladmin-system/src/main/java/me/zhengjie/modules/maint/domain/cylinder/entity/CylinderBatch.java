package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@TableName("cylinder_batch")
@Schema(description = "气瓶生产批次表")
public class CylinderBatch {
    @TableId(type = IdType.AUTO)
    @Schema(description = "批次ID")
    private Long id;
    
    @Schema(description = "生产批次号")
    private String batchNo;
    
    @Schema(description = "制造商ID")
    private Long manufacturerId;
    
    @Schema(description = "生产日期")
    private Date produceDate;
    
    @Schema(description = "生产数量")
    private Integer quantity;
    
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}