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
package me.zhengjie.modules.maint.rest.backend;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.AppPermissionService;
import me.zhengjie.modules.maint.domain.cylinder.AppRoleService;
import me.zhengjie.modules.maint.domain.cylinder.AppUserRoleService;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppPermission;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppRole;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppPermissionMapper;
import me.zhengjie.modules.maint.domain.dto.AppPermissionSaveDto;
import me.zhengjie.modules.maint.domain.dto.AppRoleDetailDto;
import me.zhengjie.modules.maint.domain.dto.AppRoleSaveDto;
import me.zhengjie.modules.maint.domain.dto.AppUserRoleBindDto;
import me.zhengjie.modules.maint.rest.command.PageQueryReq;
import me.zhengjie.modules.maint.util.SecurityContext;
import me.zhengjie.sys.ResponseResult;
import me.zhengjie.utils.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * app端用户角色权限控制
 * @author koma at cylinder-backend
 * @since 2026/3/25
 */
@Api(tags = "app端用户角色权限控制")
@Slf4j
@RestController
@RequestMapping("/api/admin/app-rbac")
@RequiredArgsConstructor
public class AppRbacController {
    
    private final AppRoleService appRoleService;
    private final AppUserRoleService appUserRoleService;
    private final AppPermissionService appPermissionService;
    
    /**
     * 新增/更新 app角色权限
     */
    @PostMapping("/permission/save")
    @ApiOperation("新增/更新 app角色权限")
    @Valid
    // @PreAuthorize("@el.check('appRole:edit')")
    public ResponseResult<Boolean> savePermission(@RequestBody AppPermissionSaveDto dto) {
        appPermissionService.saveOrUpdatePermission(dto);
        return ResponseResult.success(Boolean.TRUE);
    }
    
    /**
     * 获取全局的所有 APP 权限列表
     */
    @PostMapping("/permissions")
    @ApiOperation("获取全局的所有 APP 账号权限列表")
    // @PreAuthorize("@el.check('appRole:list')")
    public ResponseResult<List<AppPermission>> getAllPermissions() {
        return ResponseResult.success(appPermissionService.list(null));
    }
    
    /**
     * 分页查询企业下所有 APP 角色及对应权限
     */
    @PostMapping("/role/list")
    @ApiOperation("分页查询企业下所有 APP 角色及对应权限")
    @Valid
    // @PreAuthorize("@el.check('appRole:list')")
    public ResponseResult<PageResult<AppRoleDetailDto>> listRoles(@RequestBody PageQueryReq req) {
        // 获取当前登录人的所属企业
        Long companyId = SecurityContext.getCompanyId();
        PageResult<AppRoleDetailDto> pageData = appRoleService.listRolesWithPermissions(req, companyId);
        return ResponseResult.success(pageData);
    }
    
    /**
     * 新增/修改 APP 角色 (并绑定权限)
     */
    @PostMapping("/role/save")
    @ApiOperation("新增/修改 APP 角色 (并绑定权限)")
    // @PreAuthorize("@el.check('appRole:add', 'appRole:edit')")
    @Valid
    public ResponseResult<Boolean> saveRole(@RequestBody AppRoleSaveDto dto) {
        Long companyId = SecurityContext.getCompanyId();
        dto.setCompanyId(companyId);
        appRoleService.saveOrUpdateRole(dto);
        return ResponseResult.success(Boolean.TRUE);
    }
    
    /**
     * 为 APP 员工分配角色
     */
    @PostMapping("/user/bind-roles")
    @ApiOperation("为 APP 员工分配角色")
    // @PreAuthorize("@el.check('appUser:edit')")
    @Valid
    public ResponseResult<Boolean> bindUserRoles(@RequestBody AppUserRoleBindDto dto) {
        appUserRoleService.bindUserRoles(dto);
        return ResponseResult.success(Boolean.TRUE);
    }
}
