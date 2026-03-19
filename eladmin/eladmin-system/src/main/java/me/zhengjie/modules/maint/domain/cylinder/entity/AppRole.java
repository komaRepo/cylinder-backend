package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@TableName("role")
@Schema(description = "角色表")
public class AppRole {
    @TableId(type = IdType.AUTO)
    @Schema(description = "角色ID")
    private Long id;
    
    @Schema(description = "角色名称")
    private String name;
    
    @Schema(description = "企业ID")
    private Long companyId;
    
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}