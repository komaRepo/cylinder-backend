package me.zhengjie.modules.maint.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import me.zhengjie.modules.maint.domain.enums.CylinderStatus;

import java.util.Date;

@Data
@Schema(description = "管理端：气瓶列表返回实体")
public class CylinderPageDto {
    private Long id;
    private String qrcode;
    private String spec;
    private Double volume;
    private CylinderStatus currentStatus;
    private Date nextInspectionDate;
    
    // 核心翻译字段！
    private Long currentCompanyId;
    @Schema(description = "当前所属企业名称")
    private String currentCompanyName; 
    
    private Long manufacturerId;
    @Schema(description = "制造商名称")
    private String manufacturerName;
    
    private Date createTime;
}