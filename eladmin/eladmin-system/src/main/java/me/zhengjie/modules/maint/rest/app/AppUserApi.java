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
package me.zhengjie.modules.maint.rest.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.annotation.rest.AnonymousAccess;
import me.zhengjie.annotation.rest.AnonymousDeleteMapping;
import me.zhengjie.config.properties.RsaProperties;
import me.zhengjie.modules.maint.domain.cylinder.AppUserService;
import me.zhengjie.modules.maint.domain.dto.LoginVo;
import me.zhengjie.modules.maint.rest.command.AppUserLoginReq;
import me.zhengjie.modules.maint.rest.command.UserRegisterReq;
import me.zhengjie.modules.security.security.TokenProvider;
import me.zhengjie.modules.security.service.OnlineUserService;
import me.zhengjie.modules.system.domain.dto.UserPassVo;
import me.zhengjie.sys.ResponseResult;
import me.zhengjie.utils.RsaUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 *
 * @author koma at cylinder-backend
 * @since 2026/3/23
 */
@Api(tags = "APP：用户管理")
@Slf4j
@RestController
@RequestMapping("/api/app/user")
@RequiredArgsConstructor
public class AppUserApi {
    
    private final AppUserService appUserService;
    private final TokenProvider tokenProvider;
    private final OnlineUserService onlineUserService;
    
    @ApiOperation("APP用户注册")
    @PostMapping("register")
    @Valid
    @AnonymousAccess
    public ResponseResult<Boolean> register(@RequestBody UserRegisterReq req) {
        String password = null;
        try {
            password = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey,req.getPassword());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        req.setPassword(password);
        appUserService.register(req.getUsername(), req.getPassword(), req.getPhone(), req.getCompanyId());
        return ResponseResult.success(Boolean.TRUE);
    }
    
    /**
     * APP 端用户登录
     */
    @ApiOperation("APP用户登录")
    @PostMapping("/login")
    @AnonymousAccess
    @Valid
    public ResponseResult<LoginVo> login(@RequestBody AppUserLoginReq dto, HttpServletRequest request) {
        String password = null;
        try {
            password = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey, dto.getPassword());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        dto.setPassword(password);
        LoginVo loginVo = appUserService.login(dto, request);
        
        return ResponseResult.success(loginVo);
    }
    
    /**
     * APP 端用户修改密码
     */
    @ApiOperation("APP用户修改密码")
    @PostMapping("/changePwd")
    @Valid
    public ResponseResult<Boolean> changePwd(@RequestBody UserPassVo req) throws Exception {
        String oldPass = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey,req.getOldPass());
        String newPass = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey,req.getNewPass());
        appUserService.changePwd(oldPass, newPass);
        
        return ResponseResult.success(Boolean.TRUE);
    }
    
    
    @ApiOperation("退出登录")
    @PostMapping(value = "/logout")
    public ResponseResult<Boolean> logout(HttpServletRequest request) {
        String token = tokenProvider.getToken(request);
        onlineUserService.logout(token);
        return ResponseResult.success(Boolean.TRUE);
    }
    
}
