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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.CompanyService;
import me.zhengjie.modules.maint.rest.command.CompanyRegisterCmd;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 管理端企业接口
 * @author koma at cylinder-backend
 * @since 2026/3/21
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/company")
@RequiredArgsConstructor
public class CompanyController {
    
    private final CompanyService companyService;
    
    
    /**
     * 管理端企业注册
     * @param cmd
     * @return
     */
    @PostMapping("register")
    @Valid
    public ResponseEntity<Object> register(@RequestBody CompanyRegisterCmd cmd) {
        log.info("register company: {}", cmd);
        companyService.register(cmd.getType(),
                cmd.getName(),
                cmd.getCreditCode(),
                cmd.getLegalName(),
                cmd.getLegalCode(),
                cmd.getContactName(),
                cmd.getContactPhone(),
                cmd.getProvince(),
                cmd.getCity(),
                cmd.getDistrict(),
                cmd.getAddress(),
                cmd.getParentId(),
                cmd.getBusinessLicense(),
                cmd.getDangerBusinessLicense(),
                cmd.getCylinderFillLicense(),
                cmd.getSpecialEquipmentLicense());
        return ResponseEntity.ok(Boolean.TRUE);
    }
    
}
