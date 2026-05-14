package me.zhengjie.modules.maint.rest.backend;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.SysUserCompanyService;
import me.zhengjie.modules.maint.rest.command.CompanyAccountRegisterCmd;
import me.zhengjie.sys.ResponseResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Api(tags = "系统：用户企业绑定管理")
@RestController
@RequestMapping("/api/admin/bind")
@RequiredArgsConstructor
public class UserCompanyBindController {

    private final SysUserCompanyService bindService;

    /**
     * 为后台账号绑定企业
     * @param targetUserId 要绑定的 eladmin sys_user 的 ID
     * @param targetCompanyId 目标企业 ID
     */
    @PostMapping("/{targetUserId}/{targetCompanyId}")
    // @PreAuthorize("@el.check('company:bind')")
    public ResponseResult<Boolean> bindUserAndCompany(
            @PathVariable Long targetUserId, 
            @PathVariable Long targetCompanyId) {
            
        bindService.bind(targetUserId, targetCompanyId);
        return ResponseResult.success(Boolean.TRUE);
    }

    /**
     * 在企业列表为企业注册后台账号，并自动完成部门、角色、数据权限和企业绑定。
     */
    @PostMapping("/account")
    // @PreAuthorize("@el.check('company:bind')")
    public ResponseResult<Map<String, Long>> registerCompanyAccount(@Validated @RequestBody CompanyAccountRegisterCmd cmd) {
        Long userId = bindService.registerCompanyAccount(cmd);
        return ResponseResult.success(Collections.singletonMap("userId", userId));
    }
}
