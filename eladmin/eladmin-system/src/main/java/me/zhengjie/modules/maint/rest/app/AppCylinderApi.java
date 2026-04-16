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
import me.zhengjie.modules.maint.domain.cylinder.CylinderService;
import me.zhengjie.modules.maint.domain.dto.CylinderFillDto;
import me.zhengjie.modules.maint.domain.dto.CylinderFlowDto;
import me.zhengjie.modules.maint.domain.dto.CylinderOperateDto;
import me.zhengjie.sys.ResponseResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 *
 * @author koma at cylinder-backend
 * @since 2026/3/25
 */
@Api(tags = "APP：气瓶流转相关接口")
@Slf4j
@RestController
@RequestMapping("/api/app/cylinder")
@RequiredArgsConstructor
public class AppCylinderApi {
    
    private final CylinderService cylinderService;
    
    
    /**
     * 扫码出库 (发货)
     * 制造商、经销商、充装站 都有这个权限
     */
    @ApiOperation("扫码出库")
    @PostMapping("/out")
    @PreAuthorize("@el.check('app:cylinder:out')")
    @Valid
    public ResponseResult<Boolean> scanOut(@RequestBody CylinderFlowDto dto) {
        if (dto.getTargetCompanyId() == null) {
            return ResponseResult.error("出库操作必须指定接收方企业(targetCompanyId)");
        }
        cylinderService.scanOut(dto);
        return ResponseResult.success(Boolean.TRUE);
    }
    
    /**
     * 扫码入库 (收货)
     * 制造商、经销商、充装站 都有这个权限
     */
    @ApiOperation("扫码入库")
    @PostMapping("/in")
    @PreAuthorize("@el.check('app:cylinder:in')")
    @Valid
    public ResponseResult<Boolean> scanIn(@RequestBody CylinderFlowDto dto) {
        cylinderService.scanIn(dto);
        return ResponseResult.success(Boolean.TRUE);
    }
    
    
    /**
     * 扫码充气 只有充装站有这个权限
     */
    @ApiOperation("扫码充气")
    @PostMapping("/fill")
    @PreAuthorize("@el.check('app:cylinder:fill')")
    @Valid
    public ResponseResult<Boolean> fillCylinder(@RequestBody CylinderFillDto dto) {
        cylinderService.fillCylinder(dto);
        return ResponseResult.success(Boolean.TRUE);
    }
    
    
    /**
     * 扫码年检
     * 检验机构、部分具有特检资质的充装站 有此权限
     */
    @ApiOperation("扫码登记年检信息")
    @PostMapping("/inspect")
    @PreAuthorize("@el.check('app:cylinder:inspect')")
    public ResponseResult<Boolean> inspectCylinder(@Valid @RequestBody CylinderOperateDto dto) {
        cylinderService.inspectCylinder(dto);
        return ResponseResult.success(Boolean.TRUE);
    }
    
    /**
     * 扫码报废
     * 具有销毁资质的企业可操作，一旦报废，气瓶终结
     */
    @ApiOperation("扫码气瓶报废")
    @PostMapping("/scrap")
    @PreAuthorize("@el.check('app:cylinder:scrap')")
    public ResponseResult<Boolean> scrapCylinder(@Valid @RequestBody CylinderOperateDto dto) {
        cylinderService.scrapCylinder(dto);
        return ResponseResult.success(Boolean.TRUE);
    }
    
    /**
     * 扫码维修
     * 更换角阀、除锈等
     */
    @ApiOperation("扫码气瓶维修登记")
    @PostMapping("/repair")
    @PreAuthorize("@el.check('app:cylinder:repair')")
    public ResponseResult<Boolean> repairCylinder(@Valid @RequestBody CylinderOperateDto dto) {
        // 记录维修流水日志，方便日后溯源追责
        cylinderService.repairCylinder(dto);
        return ResponseResult.success(Boolean.TRUE);
    }
    
}
