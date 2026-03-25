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
package me.zhengjie.modules.maint.rest.backend;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.AppUserService;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppUser;
import me.zhengjie.modules.maint.domain.dto.AppUserCmd;
import me.zhengjie.modules.maint.domain.dto.AppUserDetail;
import me.zhengjie.sys.ResponseResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * TODO
 *
 * @author koma at cylinder-backend
 * @since 2026/3/23
 */
@Api(tags = "系统：APP用户管理")
@Slf4j
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AppUserController {
    
    private final AppUserService appUserService;
    
    
    /**
     * app用户账号列表
     * @return
     */
    @ApiOperation("APP用户列表")
    @PostMapping("/list")
    @Valid
    public ResponseResult<Page<AppUserDetail>> activeList(@RequestBody AppUserCmd cmd) {
        Page<AppUserDetail> details = appUserService.fetchUserList(
                cmd.getUsername(),
                cmd.getPhone(),
                cmd.getStatus(),
                cmd.getCreateTimeStart(),
                cmd.getCreateTimeEnd(),
                cmd.getPageAt(),
                cmd.getPageSize()
        );
        
        return ResponseResult.success(details);
    }
    
    /**
     * 获取本企业下【待激活】的 APP 员工列表
     */
    @ApiOperation("待激活APP用户列表")
    @PostMapping("/pending-list")
    @PreAuthorize("@el.check('appUser:audit')")
    public ResponseResult<Page<AppUserDetail>> getPendingPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        // 调用分页 Service
        Page<AppUserDetail> pageData = appUserService.getPendingPage(current, size);
        return ResponseResult.success(pageData);
    }
    
    /**
     * 审核通过并激活 APP 账号
     * @param id 待激活的员工账号 ID
     */
    @ApiOperation("激活APP用户账号")
    @PostMapping("/activate/{id}")
    @PreAuthorize("@el.check('appUser:audit')")
    public ResponseResult<Void> activateUser(@PathVariable("id") Long id) {
        appUserService.activateUser(id);
        return ResponseResult.success();
    }
    
}
