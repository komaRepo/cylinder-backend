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

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppUser;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppUserMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CompanyMapper;
import me.zhengjie.modules.maint.domain.dto.AppUserDetail;
import me.zhengjie.modules.maint.domain.dto.TokenDto;
import me.zhengjie.modules.maint.domain.enums.CompanyStatus;
import me.zhengjie.modules.maint.domain.enums.UserStatus;
import me.zhengjie.modules.maint.domain.enums.UserType;
import me.zhengjie.modules.maint.rest.command.AppUserLoginDto;
import me.zhengjie.modules.maint.sys.BusinessException;
import me.zhengjie.modules.security.security.TokenProvider;
import me.zhengjie.modules.security.service.OnlineUserService;
import me.zhengjie.modules.security.service.dto.AuthorityDto;
import me.zhengjie.modules.security.service.dto.JwtUserDto;
import me.zhengjie.modules.system.domain.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    
    /**
     * 获取app用户信息
     */
    public Page<AppUserDetail> fetchUserList(Long companyId, String username, String phone, UserStatus status,
                                             Date createTimeStart, Date createTimeEnd) {
        
        return this.baseMapper.fetchUserList(companyId, username, phone, status, createTimeStart, createTimeEnd);
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
            throw new BusinessException(400, "账号或密码错误");
        }
        
        // 3. 【核心业务拦截】账号状态校验
        if (ObjectUtil.equals(user.getStatus(), UserStatus.ACTIVE)) {
            throw new BusinessException(403, "您的账号正在等待企业管理员审核，暂时无法登录");
        }
        if (ObjectUtil.equals(user.getStatus(), UserStatus.SUSPENDED)) {
            throw new BusinessException(403, "您的账号已被禁用，请联系管理员");
        }
        
        // 4. 查询该用户所属的企业信息（用于组装强大的 Token 载荷）
        Company company = companyMapper.selectById(user.getCompanyId());
        if (company == null || ObjectUtil.equals(CompanyStatus.SUSPENDED, company.getStatus())) {
            throw new BusinessException(400, "您所属的企业不存在或已被禁用");
        }
        
        // 5. 组装 Spring Security 认识的 JwtUserDto
        User baseUser = new User();
        baseUser.setId(user.getId());
        baseUser.setUsername(user.getUsername());
        baseUser.setPassword(user.getPassword());
        baseUser.setEnabled(ObjectUtil.equals(user.getStatus(), UserStatus.INACTIVE));
        
        JwtUserDto jwtUser = JwtUserDto.builder()
                                       .user(baseUser)
                                       .dataScopes(new ArrayList<>())
                                       .authorities(new ArrayList<>())
                                       .userId(user.getId())
                                       .companyId(company.getId())
                                       .companyPath(company.getPath())
                                       .typeFiller(company.getTypeFiller())
                                       .typeDealer(company.getTypeDealer())
                                       .typeManufacturer(company.getTypeManufacturer())
                                       .typeInspection(company.getTypeInspection())
                                       .build();
        
        // 6. 调用你写好的神级 TokenProvider 生成 Token
        String token = tokenProvider.createToken(jwtUser);
        
        // 7. （可选）如果你原有的框架有踢人下线或严格的在线用户管理逻辑
        onlineUserService.save(jwtUser, token, request);
        
        return TokenDto.builder().token(token).build();
    }
    
    
}
