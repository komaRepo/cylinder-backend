package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("app_permission")
@Schema(description = "权限表")
public class AppPermission {

    @TableId(type = IdType.AUTO)
    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "权限代码")
    private String code;

    @Schema(description = "权限名称")
    private String name;
}