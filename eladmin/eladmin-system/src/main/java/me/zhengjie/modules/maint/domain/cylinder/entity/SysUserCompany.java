/*
 * Copyright 2026 The cylinder-backend Project under the WTFPL License,
 *
 *     http://www.wtfpl.net/about/
 *
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 * 代码千万行，注释第一行，编程不规范，日后泪两行
 *
 */
package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 企业与用户绑定关系表
 * @author koma at cylinder-backend
 * @since 2026/3/23
 */
@Data
@TableName("sys_user_company")
@Schema(description = "用户与企业关联表")
public class SysUserCompany {
    
    @TableId
    @Schema(description = "主键ID")
    private Long userId;
    
    @Schema(description = "企业ID")
    private Long companyId;
    
    @Schema(description = "创建人")
    private String createBy;
    
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
}
