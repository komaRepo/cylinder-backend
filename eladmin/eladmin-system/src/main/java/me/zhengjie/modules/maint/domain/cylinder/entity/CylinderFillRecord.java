package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@TableName("cylinder_fill_record")
@Schema(description = "气瓶充气记录")
public class CylinderFillRecord {
    @TableId(type = IdType.AUTO)
    @Schema(description = "充气记录ID")
    private Long id;
    
    @Schema(description = "气瓶ID")
    private Long cylinderId;
    
    @Schema(description = "充气企业ID")
    private Long companyId;
    
    @Schema(description = "气体类型")
    private String gasType;
    
    @Schema(description = "充气压力")
    private Double fillPressure;
    
    @Schema(description = "充气重量")
    private Double fillWeight;
    
    @Schema(description = "操作员ID")
    private Long operatorId;
    
    @Schema(description = "充气时间(分区键)")
    private Date fillTime;
    
    @Schema(description = "记录创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}