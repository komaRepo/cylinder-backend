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
import me.zhengjie.modules.maint.domain.cylinder.CompanyService;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;
import me.zhengjie.modules.maint.domain.dto.CompanyVo;
import me.zhengjie.modules.maint.rest.command.QueryCompanyListReq;
import me.zhengjie.modules.maint.util.SecurityUtils;
import me.zhengjie.sys.ResponseResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 企业接口（app端）
 * @author koma at cylinder-backend
 * @since 2026/3/26
 */
@Api(tags = "app：企业管理")
@Slf4j
@RestController
@RequestMapping("/api/app/company")
@RequiredArgsConstructor
public class CompanyApi {
    
    private final CompanyService companyService;
    
    @ApiOperation("企业列表")
    @PostMapping("listAll")
    @Valid
    @AnonymousAccess
    public ResponseResult<List<CompanyVo>> listAll() {
        log.info("query all company list");
        List<CompanyVo> list = companyService.listAll();
        return ResponseResult.success(list);
    }
    
    @ApiOperation("企业列表")
    @PostMapping("companyList")
    @Valid
    public ResponseResult<List<Company>> companyList(@RequestBody QueryCompanyListReq req) {
        log.info("query company list: {}", req);
        List<Company> list = companyService.companyList(req.getName(), req.getType(), req.getStatus());
        list.removeIf(c -> c.getId().equals(SecurityUtils.getCompanyId()));
        return ResponseResult.success(list);
    }
    
}
