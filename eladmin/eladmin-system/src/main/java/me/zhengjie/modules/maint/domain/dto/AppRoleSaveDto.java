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
package me.zhengjie.modules.maint.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 *
 * @author koma at cylinder-backend
 * @since 2026/3/25
 */
@Data
public class AppRoleSaveDto {
    // 角色ID（如果是新增则为null，修改则有值）
    private Long id;
    
    @NotBlank(message = "角色名称不能为空")
    private String name;
    
    // 前端权限树勾选的权限ID列表
    private List<Long> permissionIds;
}
