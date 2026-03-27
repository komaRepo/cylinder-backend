package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import me.zhengjie.modules.maint.domain.enums.CylinderStatus;

import java.util.Date;

@Data
@TableName("cylinder")
@Schema(description = "气瓶表")
public class Cylinder {
    @TableId(type = IdType.AUTO)
    @Schema(description = "气瓶ID")
    private Long id;
    
    @Schema(description = "气瓶唯一编号")
    private String code;
    
    @Schema(description = "二维码内容")
    private String qrcode;
    
    @Schema(description = "气瓶规格")
    private String spec;
    
    @Schema(description = "容积(L)")
    private Double volume;
    
    @Schema(description = "重量(kg)")
    private Double weight;
    
    @Schema(description = "制造日期")
    private Date manufactureDate;
    
    @Schema(description = "制造商ID")
    private Long manufacturerId;
    
    @Schema(description = "生产批次ID")
    private Long batchId;
    
    @Schema(description = "当前所属企业")
    private Long currentCompanyId;
    
    @Schema(description = "所属经销商层级路径")
    private String ownerPath;
    
    @Schema(description = "气瓶状态 0生产 1库存 2流通 3维修 4报废")
    private CylinderStatus currentStatus;
    
    @Schema(description = "最后流转记录ID")
    private Long lastFlowId;
    
    @Schema(description = "最后充气时间")
    private Date lastFillTime;
    
    @Schema(description = "最后年检时间")
    private Date lastInspectionTime;
    
    @Schema(description = "下次年检日期")
    private Date nextInspectionDate;
    
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}