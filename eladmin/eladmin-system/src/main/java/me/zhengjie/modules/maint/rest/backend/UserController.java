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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.AppUserService;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppUser;
import me.zhengjie.modules.maint.domain.dto.AppUserCmd;
import me.zhengjie.modules.maint.domain.dto.AppUserDetail;
import me.zhengjie.modules.maint.sys.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * TODO
 *
 * @author koma at cylinder-backend
 * @since 2026/3/23
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class UserController {
    
    private final AppUserService appUserService;
    
    
    /**
     * app用户账号列表
     * @return
     */
    @PostMapping("/list")
    @Valid
    public ResponseEntity<Page<AppUserDetail>> activeList(@RequestBody AppUserCmd cmd) {
        //todo 从token中获取当前用户属于哪个企业
        Long companyId = 123L;
        Page<AppUserDetail> details = appUserService.fetchUserList(
                companyId,
                cmd.getUsername(),
                cmd.getPhone(),
                cmd.getStatus(),
                cmd.getCreateTimeStart(),
                cmd.getCreateTimeEnd()
        );
        
        return ResponseEntity.ok(details);
    }
    
    /**
     * 获取本企业下【待审核】的 APP 员工列表
     */
    @PostMapping("/pending-list")
    // 这里可以加上你框架的权限标识，比如要求必须有 user:audit 权限才能访问
    @PreAuthorize("@el.check('appUser:audit')")
    public Result<Page<AppUser>> getPendingPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        // 调用分页 Service
        Page<AppUser> pageData = appUserService.getPendingPage(current, size);
        return Result.success(pageData);
    }
    
    /**
     * 审核通过并激活 APP 账号
     * @param id 待激活的员工账号 ID
     */
    @PostMapping("/activate/{id}")
    @PreAuthorize("@el.check('appUser:audit')")
    public Result<Void> activateUser(@PathVariable("id") Long id) {
        appUserService.activateUser(id);
        return Result.success();
    }
    
}
