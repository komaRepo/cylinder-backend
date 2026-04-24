package me.zhengjie.modules.maint.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("APP：气瓶状态变更操作参数")
public class CylinderOperateDto {

    @NotBlank(message = "气瓶唯一编码不能为空")
    @ApiModelProperty(value = "气瓶唯一编码", required = true)
    private String qrcode;

    @ApiModelProperty(value = "操作备注/说明 (如：角阀漏气已更换、瓶身严重变形予以报废等)")
    private String remarks;

    @ApiModelProperty(value = "证据图片URL列表 (报废或维修时强烈建议上传)")
    private List<String> imageUrls;

    // ==========================================
    // 以下为特定操作专属字段
    // ==========================================

    @ApiModelProperty(value = "下次年检日期 (仅在'年检'操作时有意义，如果不传，后端默认顺延 4 年)")
    private Date nextInspectionDate;
    
    @ApiModelProperty(value = "维修费用 (仅在'维修'操作时可用，无则不传)")
    private Double repairCost;
}