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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;
import me.zhengjie.modules.maint.domain.cylinder.entity.SysUserCompany;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CompanyMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.SysUserCompanyMapper;
import me.zhengjie.modules.security.service.dto.AuthorityDto;
import me.zhengjie.modules.security.service.dto.JwtUserDto;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.service.DataService;
import me.zhengjie.modules.system.service.RoleService;
import me.zhengjie.modules.system.service.UserService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import java.util.List;

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

    @Override
    public JwtUserDto loadUserByUsername(String username) {
        // 1. 先从 Redis 缓存里取
        JwtUserDto jwtUserDto = userCacheManager.getUserCache(username);
        
        // 2. 缓存没命中（第一次登录，或者缓存过期、被清理了）
        if(jwtUserDto == null){
            User user = userService.getLoginData(username);
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
                
                jwtUserDto = new JwtUserDto(
                        user,
                        deptIds,
                        authorities,
                        user.getId(), // 我们扩展的 userId
                        compId,       // 我们扩展的 companyId
                        compPath,     // 我们扩展的 companyPath
                        tFiller,      // 我们扩展的 typeFiller
                        tDealer,       // 我们扩展的 typeDealer
                        tManufacturer,
                        tInspection
                );
                
                // 3. 将带有企业属性的超级 DTO 存入 Redis 缓存！
                // 以后每次请求，拦截器直接从缓存反序列化出这个包含 compId 的完整对象
                userCacheManager.addUserCache(username, jwtUserDto);
            }
        }
        return jwtUserDto;
    }
}
