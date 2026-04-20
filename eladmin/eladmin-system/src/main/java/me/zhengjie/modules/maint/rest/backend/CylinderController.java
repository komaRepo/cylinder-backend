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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.CylinderService;
import me.zhengjie.modules.maint.domain.dto.CylinderDetailDto;
import me.zhengjie.modules.maint.domain.dto.CylinderExcelDto;
import me.zhengjie.modules.maint.domain.dto.CylinderPageDto;
import me.zhengjie.modules.maint.rest.command.CylinderQueryReq;
import me.zhengjie.sys.ResponseResult;
import me.zhengjie.utils.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * web端气瓶相关接口
 * @author koma at cylinder-backend
 * @since 2026/3/31
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/cylinder")
@RequiredArgsConstructor
@Api(tags = "系统：气瓶相关接口")
public class CylinderController {
    
    private final CylinderService cylinderService;
    
    /**
     * 制造商：批量导入气瓶 RFID 标签
     */
    @ApiOperation("制造商：批量导入气瓶 RFID 标签")
    @PostMapping("/import")
    // @PreAuthorize("@el.check('app:cylinder:import')")
    public ResponseResult<String> importCylinders(@RequestParam("file") MultipartFile file) {
        try {
            // 使用 EasyExcel 同步读取所有数据到内存 (1万条数据约几兆，内存完全无压力)
            List<CylinderExcelDto> excelList = EasyExcel.read(file.getInputStream())
                                                        .head(CylinderExcelDto.class)
                                                        .sheet()
                                                        .doReadSync();
            
            if (CollUtil.isEmpty(excelList)) {
                return ResponseResult.error("导入的 Excel 为空");
            }
            
            // 提取出纯字符串列表，交给 Service 处理
            List<String> rawDataList = excelList.stream()
                                                .map(CylinderExcelDto::getRfidRawData)
                                                .filter(StrUtil::isNotBlank) // 过滤掉空行
                                                .collect(Collectors.toList());
            
            // 调用核心业务逻辑，返回成功导入的数量
            int successCount = cylinderService.batchImportProduce(rawDataList);
            return ResponseResult.success("成功批量建档导入气瓶: " + successCount + " 只");
            
        } catch (Exception e) {
            return ResponseResult.error("导入失败: " + e.getMessage());
        }
    }
    
    
    /**
     * 管理端：分页查询气瓶大盘数据
     */
    @ApiOperation("管理端：分页查询气瓶数据")
    @PostMapping("/page")
    // @PreAuthorize("@el.check('cylinder:list')")
    @Valid
    public ResponseResult<PageResult<CylinderPageDto>> pageQuery(@RequestBody CylinderQueryReq req) {
        PageResult<CylinderPageDto> pageData = cylinderService.queryCylinderPage(req);
        return ResponseResult.success(pageData);
    }
    
    
    /**
     * 管理端：获取气瓶全维度溯源详情 (时间轴)
     */
    @ApiOperation("管理端：获取气瓶全维度溯源详情")
    @GetMapping("/{id}")
    // @PreAuthorize("@el.check('cylinder:list')")
    public ResponseResult<CylinderDetailDto> getDetail(@PathVariable("id") Long id) {
        CylinderDetailDto detail = cylinderService.getCylinderDetail(id);
        return ResponseResult.success(detail);
    }
    
}
