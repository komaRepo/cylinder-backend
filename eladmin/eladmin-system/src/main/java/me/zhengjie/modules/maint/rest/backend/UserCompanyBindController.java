package me.zhengjie.modules.maint.rest.backend;

import me.zhengjie.modules.maint.domain.cylinder.SysUserCompanyService;
import me.zhengjie.modules.maint.sys.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

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
    @PreAuthorize("@el.check('company:bind')")
    public Result<Boolean> bindUserAndCompany(
            @PathVariable Long targetUserId, 
            @PathVariable Long targetCompanyId) {
            
        bindService.bind(targetUserId, targetCompanyId);
        return Result.success(Boolean.TRUE);
    }
}