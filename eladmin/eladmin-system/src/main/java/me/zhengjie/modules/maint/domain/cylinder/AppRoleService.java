/*
 * Copyright 2026 The rfid-backend Project under the WTFPL License,
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
package me.zhengjie.modules.maint.domain.cylinder;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BusinessException;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppRole;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppRolePermission;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppRoleMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppRolePermissionMapper;
import me.zhengjie.modules.maint.domain.dto.AppRoleSaveDto;
import me.zhengjie.modules.maint.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppRoleService extends ServiceImpl<AppRoleMapper, AppRole> {
    
    private final AppRolePermissionMapper appRolePermissionMapper;
    
    /**
     * ================= 保存角色并绑定权限 =================
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateRole(AppRoleSaveDto dto) {
        Long myAdminCompanyId = SecurityUtils.getCompanyId();
        
        AppRole role = new AppRole();
        role.setName(dto.getName());
        role.setCompanyId(myAdminCompanyId);
        
        if (dto.getId() == null) {
            // 1. 新增角色
            this.baseMapper.insert(role);
        } else {
            // 2. 修改角色 (防越权校验：只能修改自己公司的角色)
            AppRole oldRole = this.baseMapper.selectById(dto.getId());
            if (oldRole == null || !oldRole.getCompanyId().equals(myAdminCompanyId)) {
                throw new BusinessException(403, "无权修改该角色");
            }
            role.setId(dto.getId());
            this.baseMapper.updateById(role);
            
            // 【关键清理】：修改权限前，先物理删除该角色旧的全部权限绑定
            appRolePermissionMapper.delete(new LambdaQueryWrapper<AppRolePermission>()
                    .eq(AppRolePermission::getRoleId, role.getId()));
        }
        
        // 3. 批量插入新的权限绑定记录
        if (CollUtil.isNotEmpty(dto.getPermissionIds())) {
            for (Long permissionId : dto.getPermissionIds()) {
                AppRolePermission arp = new AppRolePermission();
                arp.setRoleId(role.getId());
                arp.setPermissionId(permissionId);
                appRolePermissionMapper.insert(arp);
            }
        }
    }
    
}
