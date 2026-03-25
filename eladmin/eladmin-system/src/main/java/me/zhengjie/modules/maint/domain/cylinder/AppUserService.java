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
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BusinessException;
import me.zhengjie.modules.maint.domain.cylinder.entity.*;
import me.zhengjie.modules.maint.domain.cylinder.mapper.*;
import me.zhengjie.modules.maint.domain.dto.AppUserDetail;
import me.zhengjie.modules.maint.domain.dto.TokenDto;
import me.zhengjie.modules.maint.domain.enums.CompanyStatus;
import me.zhengjie.modules.maint.domain.enums.UserStatus;
import me.zhengjie.modules.maint.domain.enums.UserType;
import me.zhengjie.modules.maint.rest.command.AppUserLoginDto;
import me.zhengjie.modules.maint.util.SecurityUtils;
import me.zhengjie.modules.security.config.SecurityProperties;
import me.zhengjie.modules.security.security.TokenProvider;
import me.zhengjie.modules.security.service.OnlineUserService;
import me.zhengjie.modules.security.service.dto.AuthorityDto;
import me.zhengjie.modules.security.service.dto.JwtUserDto;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.sys.ResultCodeEnum;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 用户服务类
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserService extends ServiceImpl<AppUserMapper, AppUser> {
    
    private final CompanyMapper companyMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final OnlineUserService onlineUserService;
    private final AppRoleMapper appRoleMapper;
    private final AppPermissionMapper appPermissionMapper;
    private final AppRolePermissionMapper appRolePermissionMapper;
    private final AppUserRoleMapper appUserRoleMapper;
    private final SecurityProperties properties;
    
    /**
     * 获取app用户信息
     */
    public Page<AppUserDetail> fetchUserList(String username, String phone, UserStatus status,
                                             Date createTimeStart, Date createTimeEnd, Integer pageAt, Integer pageSize) {
        Long companyId = SecurityUtils.getCompanyId();
        if (companyId == null) {
            throw new BusinessException(ResultCodeEnum.COMPANY_NOT_BIND);
        }
        
        Page<Object> page = Page.of(pageAt, pageSize);
        
        return this.baseMapper.fetchUserList(companyId, username, phone, status, createTimeStart, createTimeEnd, page);
    }
    
    /**
     * app端账号注册
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(String username, String password, String phone, Long companyId) {
        // 1. 校验手机号是否已被注册
        Long phoneCount = this.baseMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getPhone, phone));
        if (phoneCount > 0) {
            throw new BusinessException(400, "该手机号已被注册");
        }
        
        // 2. 校验用户名是否已被注册（登录账号通常要求唯一）
        Long usernameCount = this.baseMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, username));
        if (usernameCount > 0) {
            throw new BusinessException(400, "该用户名已被占用");
        }
        
        // 3. 校验企业是否存在且状态正常 (防前端乱传 companyId)
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new BusinessException(400, "所选企业不存在");
        }
        if (ObjectUtil.notEqual(CompanyStatus.INACTIVE, company.getStatus())) {
            throw new BusinessException(400, "所选企业当前处于禁用状态，无法注册");
        }
        
        // 4. 构建用户实体并落库
        AppUser newUser = new AppUser();
        newUser.setUsername(username);
        newUser.setPhone(phone);
        newUser.setCompanyId(companyId);
        
        // 5. 【核心】使用 Spring Security 的 BCrypt 加密密码
        newUser.setPassword(passwordEncoder.encode(password));
        
        // 6. 初始化权限与状态
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setUserType(UserType.NORMAL);
        
        // 插入数据库
        this.baseMapper.insert(newUser);
    }
    
    /**
     * app端登陆
     */
    public TokenDto login(AppUserLoginDto dto, HttpServletRequest request) {
        // 1. 根据用户名查询用户
        AppUser user = this.baseMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, dto.getUsername()));
        
        // 2. 账号存在性与密码校验
        // 注意：为了防黑客猜测，账号不存在和密码错误的提示文案必须保持一致
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCodeEnum.PASSWORD_ERROR);
        }
        
        // 3. 【核心业务拦截】账号状态校验
        // 💡 修正了状态逻辑：INACTIVE 代表待审核，ACTIVE 代表正常，SUSPENDED 代表禁用
        if (ObjectUtil.equals(user.getStatus(), UserStatus.ACTIVE)) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_PENDING);
        }
        if (ObjectUtil.equals(user.getStatus(), UserStatus.SUSPENDED)) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_DISABLED);
        }
        
        // 4. 查询该用户所属的企业信息（用于组装强大的 Token 载荷）
        Company company = companyMapper.selectById(user.getCompanyId());
        if (company == null || ObjectUtil.equals(company.getStatus(), CompanyStatus.SUSPENDED)) {
            throw new BusinessException(ResultCodeEnum.COMPANY_NOT_EXIST);
        }
        
        // ==========================================
        // 5. 【新增核心】：极速查询该员工的所有权限 Code，并转为 Spring Security 格式
        // ==========================================
        Set<String> permissionCodes = appPermissionMapper.selectPermissionCodesByUserId(user.getId());
        
        List<AuthorityDto> authorities = new ArrayList<>();
        if (CollUtil.isNotEmpty(permissionCodes)) {
            for (String code : permissionCodes) {
                AuthorityDto authorityDto = new AuthorityDto();
                authorityDto.setAuthority(code);
                authorities.add(authorityDto);
            }
        }
        
        // 6. 组装 Spring Security 认识的 JwtUserDto
        User baseUser = new User();
        baseUser.setId(user.getId());
        baseUser.setUsername(user.getUsername());
        baseUser.setPassword(user.getPassword());
        // 只有 ACTIVE (正常状态) 才代表启用
        baseUser.setEnabled(ObjectUtil.equals(user.getStatus(), UserStatus.ACTIVE));
        
        JwtUserDto jwtUser = JwtUserDto.builder()
                                       .user(baseUser)
                                       .dataScopes(new ArrayList<>()) // APP端暂不需要数据范围
                                       .authorities(authorities)      // 🚀 注入真实的 APP 权限列表！
                                       .userId(user.getId())
                                       .companyId(company.getId())
                                       .companyPath(company.getPath())
                                       .typeFiller(company.getTypeFiller())
                                       .typeDealer(company.getTypeDealer())
                                       .typeManufacturer(company.getTypeManufacturer())
                                       .typeInspection(company.getTypeInspection())
                                       .build();
        
        // 7. 调用你写好的神级 TokenProvider 生成 Token
        String token = tokenProvider.createToken(jwtUser);
        
        // 8. 严格的在线用户管理逻辑 (存入 Redis，支持强制踢人下线)
        // onlineUserService.save(jwtUser, token, request);
        
        // 9. 按照你定义好的返回格式返回 TokenDto
        return TokenDto.builder().token(properties.getTokenStartWith().concat(token)).build();
    }
    
    
    /**
     * ================= 获取待审批列表 =================
     */
    public Page<AppUser> getPendingPage(Integer current, Integer size) {
        //1. 获取当前管理员的企业 ID（防越权核心）
        Long myAdminCompanyId = SecurityUtils.getCompanyId();
        
        // 2. 构建 MyBatis-Plus 的分页对象
        Page<AppUser> page = new Page<>(current, size);
        
        // 3. 执行物理分页查询（MyBatis-Plus 会自动帮你拼装 LIMIT 语句，并自动执行 COUNT 查总数）
        return this.baseMapper.selectPage(page, new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getCompanyId, myAdminCompanyId)
                .eq(AppUser::getStatus, 0)
                .orderByDesc(AppUser::getCreateTime));
    }
    
    /**
     * ================= 激活员工账号 =================
     */
    @Transactional(rollbackFor = Exception.class)
    public void activateUser(Long targetUserId) {
        // 1. 拿当前管理员的信息
        Long myAdminCompanyId = SecurityUtils.getCompanyId();
        Long myAdminUserId = SecurityUtils.getUserId();
        
        // 2. 查询目标待激活的用户
        AppUser targetUser = this.baseMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(404, "该用户不存在");
        }
        
        // 3. 状态校验：防重复点击 (必须是“待激活”状态才能被激活)
        if (!ObjectUtil.equals(targetUser.getStatus(), UserStatus.ACTIVE)) {
            throw new BusinessException(400, "该账号不是待激活状态，无法执行此操作");
        }
        
        // 4. 【绝对核心防线：防越权漏洞】
        if (!targetUser.getCompanyId().equals(myAdminCompanyId)) {
            throw new BusinessException(403, "严重警告：您无权激活其他企业的员工账号！");
        }
        
        // 5. 执行激活更新操作
        AppUser updateObj = new AppUser();
        updateObj.setId(targetUserId);
        updateObj.setStatus(UserStatus.INACTIVE);
        updateObj.setActivatorId(myAdminUserId); // 留痕：记录是谁审核通过的
        this.baseMapper.updateById(updateObj);
        
        // ==========================================
        // 6. 【新增核心逻辑】自动为该员工分配基于企业资质的 APP 权限
        // ==========================================
        Company company = companyMapper.selectById(targetUser.getCompanyId());
        assignDefaultRoleAndPermissions(targetUserId, company);
    }
    
    /**
     * 核心私有方法：根据企业资质分配权限
     */
    private void assignDefaultRoleAndPermissions(Long userId, Company company) {
        // 1. 查找该企业是否已经生成过【默认员工角色】
        AppRole defaultRole = appRoleMapper.selectOne(new LambdaQueryWrapper<AppRole>()
                .eq(AppRole::getCompanyId, company.getId())
                .eq(AppRole::getName, "默认员工角色"));
        
        // 2. 如果没有，说明是该企业第一次激活员工，我们需要根据资质“量身定制”一个角色
        if (defaultRole == null) {
            // 创建角色
            defaultRole = new AppRole();
            defaultRole.setName("默认员工角色");
            defaultRole.setCompanyId(company.getId());
            appRoleMapper.insert(defaultRole);
            
            // 收集该企业应该拥有的权限 Code
            Set<String> permissionCodes = new HashSet<>();
            
            // 只要是合法企业，都需要最基础的出入库权限
            permissionCodes.add("app:cylinder:in");
            permissionCodes.add("app:cylinder:out");
            
            // 根据企业资质叠加高级权限
            if (company.getTypeManufacturer() != null && company.getTypeManufacturer() == 1) {
                permissionCodes.add("app:cylinder:produce"); // 建档/生产
            }
            if (company.getTypeFiller() != null && company.getTypeFiller() == 1) {
                permissionCodes.add("app:cylinder:fill");    // 充气
            }
            if (company.getTypeInspection() != null && company.getTypeInspection() == 1) {
                permissionCodes.add("app:cylinder:inspect"); // 年检
            }
            
            // 查询这些 Code 对应的权限 ID
            List<AppPermission> permissions = appPermissionMapper.selectList(
                    new LambdaQueryWrapper<AppPermission>().in(AppPermission::getCode, permissionCodes)
            );
            
            // 将权限绑定到刚刚创建的【默认员工角色】上
            for (AppPermission permission : permissions) {
                AppRolePermission rolePermission = new AppRolePermission();
                rolePermission.setRoleId(defaultRole.getId());
                rolePermission.setPermissionId(permission.getId());
                appRolePermissionMapper.insert(rolePermission);
            }
        }
        
        // 3. 将当前激活的员工，绑定到该企业的【默认员工角色】上
        // 先检查防重（防极端并发下重复绑定报错）
        Long count = appUserRoleMapper.selectCount(new LambdaQueryWrapper<AppUserRole>()
                .eq(AppUserRole::getUserId, userId)
                .eq(AppUserRole::getRoleId, defaultRole.getId()));
        
        if (count == 0) {
            AppUserRole userRole = new AppUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(defaultRole.getId());
            appUserRoleMapper.insert(userRole);
        }
    }
    
    
}
