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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.OperationLogService;
import me.zhengjie.modules.maint.domain.cylinder.ScanRecordService;
import me.zhengjie.modules.maint.domain.dto.OperationLogPageDto;
import me.zhengjie.modules.maint.domain.dto.ScanRecordPageDto;
import me.zhengjie.modules.maint.rest.command.OperationLogQueryReq;
import me.zhengjie.modules.maint.rest.command.ScanRecordQueryReq;
import me.zhengjie.sys.ResponseResult;
import me.zhengjie.utils.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 日志相关接口
 * @author koma at cylinder-backend
 * @since 2026/4/29
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/log")
@RequiredArgsConstructor
@Api(tags = "系统：日志相关接口")
public class LogController {

    private final OperationLogService operationLogService;
    private final ScanRecordService scanRecordService;

    /**
     * 分页查询操作日志
     */
    @ApiOperation("分页查询操作日志")
    @PostMapping("/operation/page")
    // @PreAuthorize("@el.check('log:operation:list')")
    @Valid
    public ResponseResult<PageResult<OperationLogPageDto>> queryOperationLogPage(@RequestBody OperationLogQueryReq req) {
        PageResult<OperationLogPageDto> pageData = operationLogService.queryOperationLogPage(req);
        return ResponseResult.success(pageData);
    }

    /**
     * 分页查询扫码记录
     */
    @ApiOperation("分页查询扫码记录")
    @PostMapping("/scan/page")
    // @PreAuthorize("@el.check('log:scan:list')")
    @Valid
    public ResponseResult<PageResult<ScanRecordPageDto>> queryScanRecordPage(@RequestBody ScanRecordQueryReq req) {
        PageResult<ScanRecordPageDto> pageData = scanRecordService.pageQuery(req);
        return ResponseResult.success(pageData);
    }
}
