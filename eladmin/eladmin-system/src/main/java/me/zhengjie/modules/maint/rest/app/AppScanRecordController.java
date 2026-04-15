package me.zhengjie.modules.maint.rest.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.ScanRecordService;
import me.zhengjie.modules.maint.domain.dto.ScanRecordPageDto;
import me.zhengjie.modules.maint.rest.command.ScanRecordQueryReq;
import me.zhengjie.sys.ResponseResult;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.PageUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Api(tags = "系统：扫码记录接口")
@Slf4j
@RestController
@RequestMapping("/api/admin/scan-record")
@RequiredArgsConstructor
public class AppScanRecordController {

    private final ScanRecordService scanRecordService;

    @ApiOperation("扫码记录分页查询")
    @PostMapping("/page")
    // @PreAuthorize("@el.check('app:scanRecord:list')")
    @Valid
    public ResponseResult<PageResult<ScanRecordPageDto>> pageQuery(@RequestBody ScanRecordQueryReq req) {
        Page<ScanRecordPageDto> pageData = scanRecordService.pageQuery(req);
        return ResponseResult.success(PageUtil.toPage(pageData.getRecords(), pageData.getTotal()));
    }
}
