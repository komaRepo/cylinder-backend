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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.CylinderService;
import me.zhengjie.sys.ResponseResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 *
 * @author koma at cylinder-backend
 * @since 2026/3/25
 */
@Slf4j
@RestController
@RequestMapping("/api/app/cylinder")
@RequiredArgsConstructor
public class AppCylinderApi {
    
    private final CylinderService cylinderService;
    
    /**
     * 扫码充气
     */
    @PostMapping("/fill")
    @PreAuthorize("@el.check('app:cylinder:fill')")
    @Valid
    public ResponseResult<Object> scanFill() {
        // log.info("扫码充气请求: {}", req);
        // cylinderService.fill(dto);
        return ResponseResult.success();
    }
    
    /**
     * 扫码出库
     */
    @PostMapping("/scan-out")
    @PreAuthorize("@el.check('app:cylinder:out')")
    @Valid
    public ResponseResult<Void> scanOut() {
        // log.info("扫码出库请求: {}", req);
        // cylinderService.scanOut(dto);
        return ResponseResult.success();
    }
}
