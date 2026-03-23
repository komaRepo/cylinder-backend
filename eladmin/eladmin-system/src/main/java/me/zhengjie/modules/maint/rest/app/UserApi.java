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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.annotation.rest.AnonymousAccess;
import me.zhengjie.modules.maint.domain.cylinder.AppUserService;
import me.zhengjie.modules.maint.domain.dto.TokenDto;
import me.zhengjie.modules.maint.rest.command.AppUserLoginDto;
import me.zhengjie.modules.maint.rest.command.UserRegisterReq;
import me.zhengjie.modules.maint.sys.Result;
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
@Slf4j
@RestController
@RequestMapping("/api/app/user")
@RequiredArgsConstructor
public class UserApi {
    
    private final AppUserService appUserService;
    
    @PostMapping("register")
    @Valid
    @AnonymousAccess
    public Result<Object> register(@RequestBody UserRegisterReq req) {
        appUserService.register(req.getUsername(), req.getPassword(), req.getPhone(), req.getCompanyId());
        return Result.success(Boolean.TRUE);
    }
    
    /**
     * APP 端用户登录
     */
    @PostMapping("/login")
    @AnonymousAccess
    @Valid
    public Result<Object> login(@RequestBody AppUserLoginDto dto, HttpServletRequest request) {
        // 返回 token 和必要的用户信息
        TokenDto token = appUserService.login(dto, request);
        
        return Result.success(token);
    }
    
}
