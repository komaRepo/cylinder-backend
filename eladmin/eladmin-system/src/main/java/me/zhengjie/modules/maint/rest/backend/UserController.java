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
import me.zhengjie.modules.maint.domain.dto.AppUserCmd;
import me.zhengjie.modules.maint.domain.dto.AppUserDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/activeList")
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
    
}
