package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import me.zhengjie.modules.maint.domain.enums.UserStatus;
import me.zhengjie.modules.maint.domain.enums.UserType;

import java.util.Date;

@Data
@TableName("app_user")
@Schema(description = "用户表")
public class AppUser {
    @TableId(type = IdType.AUTO)
    @Schema(description = "用户ID")
    private Long id;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "密码")
    private String password;
    
    @Schema(description = "手机号")
    private String phone;
    
    @Schema(description = "企业ID")
    private Long companyId;
    
    @Schema(description = "用户类型 1企业管理员 2普通员工")
    private UserType userType;
    
    @Schema(description = "状态 0待激活 1正常 2禁用")
    private UserStatus status;
    
    @Schema(description = "激活人ID")
    private Long activatorId;
    
    @Schema(description = "最后登录时间")
    private Date lastLogin;
    
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}