package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import me.zhengjie.modules.maint.domain.enums.FlowType;

import java.util.Date;

@Data
@TableName("cylinder_flow")
@Schema(description = "气瓶流转记录")
public class CylinderFlow {
    @TableId(type = IdType.AUTO)
    @Schema(description = "流转ID")
    private Long id;
    
    @Schema(description = "气瓶ID")
    private Long cylinderId;
    
    @Schema(description = "批量流转流水号")
    private String batchFlowNo;
    
    @Schema(description = "来源企业ID")
    private Long fromCompanyId;
    
    @Schema(description = "目标企业ID")
    private Long toCompanyId;
    
    @Schema(description = "流转类型 1制造商出库 2经销商入库 3经销商转下级 4退回 5回收")
    private Integer type;
    
    @Schema(description = "操作人ID")
    private Long operatorId;
    
    @Schema(description = "备注")
    private String remark;
    
    @Schema(description = "操作时间(分区键)")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}