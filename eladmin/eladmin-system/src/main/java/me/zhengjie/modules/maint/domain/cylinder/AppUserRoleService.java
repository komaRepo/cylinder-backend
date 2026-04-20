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
import me.zhengjie.modules.maint.domain.cylinder.entity.AppUser;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppUserRole;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppRoleMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppUserMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppUserRoleMapper;
import me.zhengjie.modules.maint.domain.dto.AppUserRoleBindDto;
import me.zhengjie.modules.maint.util.SecurityUtils;
import me.zhengjie.sys.ResultCodeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户角色服务类
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserRoleService extends ServiceImpl<AppUserRoleMapper, AppUserRole> {
    
    
    private final AppUserMapper appUserMapper;
    private final AppRoleMapper appRoleMapper;
    
    /**
     * ================= 为 APP 员工分配角色 =================
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindUserRoles(AppUserRoleBindDto dto) {
        Long myAdminCompanyId = SecurityUtils.getCompanyId();
        
        // 1. 校验目标员工是否存在且属于本企业
        AppUser targetUser = appUserMapper.selectById(dto.getUserId());
        if (targetUser == null || !targetUser.getCompanyId().equals(myAdminCompanyId)) {
            throw new BusinessException(ResultCodeEnum.ROLE_ASSIGN_FORBIDDEN);
        }
        
        // 2. 校验传入的 roleIds 是否合法（防止黑客传入别的企业的顶级角色ID）
        if (CollUtil.isNotEmpty(dto.getRoleIds())) {
            Long validRoleCount = appRoleMapper.selectCount(new LambdaQueryWrapper<AppRole>()
                    .in(AppRole::getId, dto.getRoleIds())
                    .eq(AppRole::getCompanyId, myAdminCompanyId));
            
            if (validRoleCount != dto.getRoleIds().size()) {
                throw new BusinessException(ResultCodeEnum.INVALID_ROLE_ASSIGN);
            }
        }
        
        // 3. 清理该员工旧的角色绑定
        this.baseMapper.delete(new LambdaQueryWrapper<AppUserRole>()
                .eq(AppUserRole::getUserId, dto.getUserId()));
        
        // 4. 插入新的角色绑定
        if (CollUtil.isNotEmpty(dto.getRoleIds())) {
            for (Long roleId : dto.getRoleIds()) {
                AppUserRole aur = new AppUserRole();
                aur.setUserId(dto.getUserId());
                aur.setRoleId(roleId);
                this.baseMapper.insert(aur);
            }
        }
    }
    
}
