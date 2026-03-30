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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BusinessException;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppPermission;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppPermissionMapper;
import me.zhengjie.modules.maint.domain.dto.AppPermissionSaveDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 权限
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppPermissionService extends ServiceImpl<AppPermissionMapper, AppPermission> {
    
    
    /**
     * 保存或修改权限
     * @param dto
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdatePermission(AppPermissionSaveDto dto) {
        
        // 【核心防线】：校验权限代码 code 的全局唯一性
        LambdaQueryWrapper<AppPermission> checkWrapper = new LambdaQueryWrapper<AppPermission>()
                .eq(AppPermission::getCode, dto.getCode());
        
        // 如果是修改操作，排除掉自己当前的 ID
        if (dto.getId() != null) {
            checkWrapper.ne(AppPermission::getId, dto.getId());
        }
        
        if (this.baseMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException(400, "该权限代码 [" + dto.getCode() + "] 已存在，请勿重复添加！");
        }
        
        // 执行保存或更新
        AppPermission permission = new AppPermission();
        permission.setId(dto.getId());
        permission.setName(dto.getName());
        permission.setCode(dto.getCode());
        
        // saveOrUpdate 是 MyBatis-Plus ServiceImpl 自带的神级方法
        // id 为 null 则 insert，有 id 则 update
        this.saveOrUpdate(permission);
    }
    
}
