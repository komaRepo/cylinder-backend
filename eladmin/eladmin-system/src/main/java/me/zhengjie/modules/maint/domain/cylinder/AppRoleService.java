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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BusinessException;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppPermission;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppRole;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppRolePermission;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppPermissionMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppRoleMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppRolePermissionMapper;
import me.zhengjie.modules.maint.domain.dto.AppRoleDetailDto;
import me.zhengjie.modules.maint.domain.dto.AppRoleSaveDto;
import me.zhengjie.modules.maint.rest.command.PageQueryReq;
import me.zhengjie.modules.maint.util.SecurityUtils;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.PageUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final AppPermissionMapper appPermissionMapper;
    
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
    
    
    /**
     * web端查询app角色信息包含授权
     */
    public PageResult<AppRoleDetailDto> listRolesWithPermissions(PageQueryReq req, Long companyId) {
        
        // ==========================================
        // 1. 第一步：仅对【角色主表】进行干净的物理分页
        // ==========================================
        Page<AppRole> page = new Page<>(req.getPage(), req.getSize());
        LambdaQueryWrapper<AppRole> queryWrapper = new LambdaQueryWrapper<AppRole>()
                .eq(AppRole::getCompanyId, companyId)
                .orderByDesc(AppRole::getCreateTime);
        
        // 如果前端传了角色名称来搜索
        // if (StrUtil.isNotBlank(req.getKeyword())) {
        //     queryWrapper.like(AppRole::getName, req.getKeyword());
        // }
        
        this.baseMapper.selectPage(page, queryWrapper);
        List<AppRole> roleRecords = page.getRecords();
        
        // 构造返回的分页对象
        Page<AppRoleDetailDto> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(roleRecords)) {
            return new PageResult<>();
        }
        
        // ==========================================
        // 2. 第二步：提取这 10 个角色的 ID，去中间表批量查出关系
        // ==========================================
        List<Long> roleIds = roleRecords.stream().map(AppRole::getId).collect(Collectors.toList());
        List<AppRolePermission> rolePermissions = appRolePermissionMapper.selectList(
                new LambdaQueryWrapper<AppRolePermission>().in(AppRolePermission::getRoleId, roleIds)
        );
        
        // ==========================================
        // 3. 第三步：提取所有的权限 ID，去权限表批量查出真实的权限数据
        // ==========================================
        List<Long> permIds = rolePermissions.stream()
                                            .map(AppRolePermission::getPermissionId)
                                            .distinct() // 去重，防止查相同的权限
                                            .collect(Collectors.toList());
        
        Map<Long, AppPermission> permissionMap = new HashMap<>();
        if (CollUtil.isNotEmpty(permIds)) {
            List<AppPermission> permissions = appPermissionMapper.selectBatchIds(permIds);
            // 转化为 ID -> AppPermission 的 Map，方便后续极速匹配
            permissionMap = permissions.stream()
                                       .collect(Collectors.toMap(AppPermission::getId, p -> p));
        }
        
        // ==========================================
        // 4. 第四步：在 Java 内存中进行完美的拼装！
        // ==========================================
        // 先按 RoleId 将权限分组
        Map<Long, List<AppPermission>> rolePermGrouping = new HashMap<>();
        for (AppRolePermission rp : rolePermissions) {
            AppPermission realPerm = permissionMap.get(rp.getPermissionId());
            if (realPerm != null) {
                rolePermGrouping.computeIfAbsent(rp.getRoleId(), k -> new ArrayList<>()).add(realPerm);
            }
        }
        
        // 最后，将原始的 Role 转换为 DTO，并塞入权限
        List<AppRoleDetailDto> dtoList = roleRecords.stream().map(role -> {
            AppRoleDetailDto dto = new AppRoleDetailDto();
            BeanUtils.copyProperties(role, dto); // 拷贝基础属性
            
            // 塞入该角色的具体权限列表
            List<AppPermission> myPerms = rolePermGrouping.getOrDefault(role.getId(), new ArrayList<>());
            dto.setPermissions(myPerms);
            
            // 塞入权限 ID 列表（纯为了前端 Vue 里的 el-tree 回显打勾方便）
            dto.setPermissionIds(myPerms.stream().map(AppPermission::getId).collect(Collectors.toList()));
            
            return dto;
        }).collect(Collectors.toList());
        
        resultPage.setRecords(dtoList);
        
        return PageUtil.toPage(dtoList, resultPage.getTotal());
    }
    
}
