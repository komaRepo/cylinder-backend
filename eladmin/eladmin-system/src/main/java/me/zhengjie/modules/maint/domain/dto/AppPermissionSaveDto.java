package me.zhengjie.modules.maint.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "新增/修改APP权限字典参数")
public class AppPermissionSaveDto {

    @Schema(description = "权限ID (新增时传 null，修改时必传)")
    private Long id;

    @NotBlank(message = "权限名称不能为空")
    private String name;

    @NotBlank(message = "权限代码不能为空")
    private String code;
}