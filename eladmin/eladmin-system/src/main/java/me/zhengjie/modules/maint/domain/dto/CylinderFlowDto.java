package me.zhengjie.modules.maint.domain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class CylinderFlowDto {

    @NotBlank(message = "气瓶唯一码(二维码)不能为空")
    private String qrcode;

    // 出库时必须传目标接收企业的 ID；入库时不需要传
    private Long targetCompanyId; 
    
    // 选填：工人在出入库时备注的信息（如：车辆川A88888、有轻微磕碰等）
    private String remark;
}