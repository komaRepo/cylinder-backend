package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@TableName("cylinder_inspection")
@Schema(description = "气瓶年检记录")
public class CylinderInspection {
    @TableId(type = IdType.AUTO)
    @Schema(description = "年检ID")
    private Long id;
    
    @Schema(description = "气瓶ID")
    private Long cylinderId;
    
    @Schema(description = "年检机构ID")
    private Long companyId;
    
    @Schema(description = "年检日期")
    private Date inspectionDate;
    
    @Schema(description = "年检结果 1合格 2不合格 3维修 4报废")
    private Integer result;
    
    @Schema(description = "下次年检日期")
    private Date nextInspectionDate;
    
    @Schema(description = "检验人员ID")
    private Long operatorId;
    
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}