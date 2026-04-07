/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.zhengjie.modules.security.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.exception.BusinessException;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppUser;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;
import me.zhengjie.modules.maint.domain.cylinder.entity.SysUserCompany;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppPermissionMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppUserMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CompanyMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.SysUserCompanyMapper;
import me.zhengjie.modules.maint.domain.enums.AccountType;
import me.zhengjie.modules.maint.domain.enums.UserStatus;
import me.zhengjie.modules.security.service.dto.AuthorityDto;
import me.zhengjie.modules.security.service.dto.JwtUserDto;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.service.DataService;
import me.zhengjie.modules.system.service.RoleService;
import me.zhengjie.modules.system.service.UserService;
import me.zhengjie.sys.ResultCodeEnum;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Zheng Jie
 * @date 2018-11-22
 */
@Slf4j
@RequiredArgsConstructor
@Service("userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserService userService;
    private final RoleService roleService;
    private final DataService dataService;
    private final UserCacheManager userCacheManager;
    private final SysUserCompanyMapper sysUserCompanyMapper;
    private final CompanyMapper companyMapper;
    
    private final AppUserMapper appUserMapper;
    private final AppPermissionMapper appPermissionMapper;
    
    @Override
    public JwtUserDto loadUserByUsername(String smartSubject) {
        
        // ==========================================
        // 1. 【原汁原味】：先从 Eladmin 的专属缓存中尝试获取
        // 注意：这里的缓存 Key 会是 "APP:zhangsan"，完美防冲突！
        // ==========================================
        JwtUserDto jwtUserDto = userCacheManager.getUserCache(smartSubject);
        
        if (jwtUserDto == null) {
            // 2. 如果缓存没命中，执行我们的智能路由查库逻辑
            if (smartSubject.startsWith(AccountType.APP.name() + ":")) {
                String realUsername = smartSubject.substring(AccountType.APP.name().length() + 1);
                jwtUserDto = loadAppUser(realUsername);
                
            } else if (smartSubject.startsWith(AccountType.ADMIN.name() + ":")) {
                String realUsername = smartSubject.substring(AccountType.ADMIN.name().length() + 1);
                jwtUserDto = loadAdminUser(realUsername);
                
            } else {
                jwtUserDto = loadAdminUser(smartSubject);
            }
            
            // ==========================================
            // 3. 【原汁原味】：查完库后，手动塞回 Eladmin 的缓存管理器！
            // ==========================================
            userCacheManager.addUserCache(smartSubject, jwtUserDto);
        }
        
        return jwtUserDto;
    }
    
    /**
     * 专属逻辑：加载移动 APP 端用户信息
     */
    private JwtUserDto loadAppUser(String username) {
        // 1. 查询 APP 用户表
        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, username));
        if (user == null) {
            throw new BusinessException("APP端用户不存在");
        }
        
        // 2. 校验账号状态（复用登录时的拦截逻辑）
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_DISABLED);
        }
        
        // 3. 查询所属企业
        Company company = companyMapper.selectById(user.getCompanyId());
        if (company == null) {
            throw new BusinessException(ResultCodeEnum.COMPANY_NOT_EXIST);
        }
        
        // 4. 查询该员工的所有权限 Code
        Set<String> permissionCodes = appPermissionMapper.selectPermissionCodesByUserId(user.getId());
        List<AuthorityDto> authorities = new ArrayList<>();
        if (CollUtil.isNotEmpty(permissionCodes)) {
            authorities = permissionCodes.stream().map(code -> {
                AuthorityDto auth = new AuthorityDto();
                auth.setAuthority(code);
                return auth;
            }).collect(Collectors.toList());
        }
        
        // 5. 组装 Spring 核心 User 对象
        User baseUser = new User();
        baseUser.setId(user.getId());
        baseUser.setUsername(user.getUsername());
        baseUser.setPassword(user.getPassword());
        baseUser.setEnabled(user.getStatus() == UserStatus.ACTIVE);
        
        // 6. 组装 JwtUserDto (确保包含所有业务字段，以便 getCurrentUser 时能取到)
        return JwtUserDto.builder()
                         .user(baseUser)
                         .userId(user.getId())
                         .accountType(AccountType.APP) // 【关键】标记为APP账号
                         .companyId(company.getId())
                         .companyPath(company.getPath())
                         .typeFiller(company.getTypeFiller())
                         .typeDealer(company.getTypeDealer())
                         .typeManufacturer(company.getTypeManufacturer())
                         .typeInspection(company.getTypeInspection())
                         .authorities(authorities)
                         .dataScopes(new ArrayList<>()) // APP端暂不涉及复杂的管理端数据脱敏
                         .build();
    }
    
    /**
     * 专属逻辑：加载 Web 管理端用户信息
     */
    private JwtUserDto loadAdminUser(String username) {
        User user = userService.getLoginData(username);
        JwtUserDto adminUser;
        if (user == null) {
            throw new BadRequestException("用户不存在");
        } else {
            if (!user.getEnabled()) {
                throw new BadRequestException("账号未激活！");
            }
            // 获取用户的原生权限和部门
            List<AuthorityDto> authorities = roleService.buildPermissions(user);
            List<Long> deptIds = dataService.getDeptIds(user);
            
            // ==========================================
            // 【新增核心业务】：查询并绑定我们自己的企业上下文
            // ==========================================
            SysUserCompany binding = sysUserCompanyMapper.selectById(user.getId());
            Company company = null;
            if (binding != null) {
                company = companyMapper.selectById(binding.getCompanyId());
            }
            
            // 安全提取企业字段，防止超级管理员(admin)未绑定企业时报空指针
            Long compId = company != null ? company.getId() : null;
            String compPath = company != null ? company.getPath() : null;
            Integer tFiller = company != null ? company.getTypeFiller() : 0;
            Integer tDealer = company != null ? company.getTypeDealer() : 0;
            Integer tManufacturer = company != null ? company.getTypeManufacturer() : 0;
            Integer tInspection = company != null ? company.getTypeInspection() : 0;
            
            adminUser = new JwtUserDto(
                    user,
                    deptIds,
                    authorities,
                    user.getId(), // 我们扩展的 userId
                    compId,       // 我们扩展的 companyId
                    compPath,     // 我们扩展的 companyPath
                    tFiller,      // 我们扩展的 typeFiller
                    tDealer,       // 我们扩展的 typeDealer
                    tManufacturer,
                    tInspection,
                    AccountType.ADMIN
            );
        }
        return adminUser;
    }
    
    
}
