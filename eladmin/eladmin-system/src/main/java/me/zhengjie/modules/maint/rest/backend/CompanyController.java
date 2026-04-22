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
import me.zhengjie.annotation.rest.AnonymousAccess;
import me.zhengjie.modules.maint.domain.cylinder.CompanyService;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;
import me.zhengjie.modules.maint.rest.command.CompanyRegisterCmd;
import me.zhengjie.modules.maint.rest.command.QueryCompanyListReq;
import me.zhengjie.sys.ResponseResult;
import me.zhengjie.utils.PageResult;
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
@Api(tags = "系统：企业管理")
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
    @ApiOperation("企业注册")
    @PostMapping("register")
    @Valid
    @AnonymousAccess
    public ResponseResult<Boolean> register(@RequestBody CompanyRegisterCmd cmd) {
        log.info("register company: {}", cmd);
        companyService.register(cmd.getType(),
                cmd.getName(),
                cmd.getCreditCode(),
                cmd.getCode(),
                cmd.getLegalName(),
                cmd.getLegalCode(),
                cmd.getContactName(),
                cmd.getContactPhone(),
                cmd.getCountryCode(),
                cmd.getProvince(),
                cmd.getCity(),
                cmd.getDistrict(),
                cmd.getAddress(),
                cmd.getParentId(),
                cmd.getBusinessLicense(),
                cmd.getDangerBusinessLicense(),
                cmd.getCylinderFillLicense(),
                cmd.getSpecialEquipmentLicense(),
                cmd.getLongitude(),
                cmd.getLatitude());
        return ResponseResult.success(Boolean.TRUE);
    }
    
    
    @ApiOperation("企业列表")
    @PostMapping("companyList")
    @Valid
    public ResponseResult<PageResult<Company>> companyList(@RequestBody QueryCompanyListReq req) {
        log.info("query company list: {}", req);
        PageResult<Company> page = companyService.companyList(req.getName(), req.getType(), req.getStatus(), req.getPage(), req.getSize());
        return ResponseResult.success(page);
    }
    
}
