package me.zhengjie.modules.maint.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Schema(description = "APP端扫码充气参数")
public class CylinderFillDto {
    
    @NotBlank(message = "气瓶唯一码(二维码)不能为空")
    private String qrcode;
    
    @NotNull(message = "充装重量不能为空")
    private Double fillWeight;
    
    @NotNull(message = "充装压力不能为空")
    private Double fillPressure;
}