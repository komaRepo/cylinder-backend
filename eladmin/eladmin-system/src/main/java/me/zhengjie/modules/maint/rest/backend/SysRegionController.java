package me.zhengjie.modules.maint.rest.backend;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.zhengjie.modules.maint.domain.cylinder.RegionLocalService;
import me.zhengjie.modules.maint.domain.dto.RegionTreeDto;
import me.zhengjie.sys.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "系统：地区接口")
@RestController
@RequestMapping("/api/sys/region")
@RequiredArgsConstructor
@Tag(name = "地区接口")
public class SysRegionController {

    private final RegionLocalService regionLocalService;

    @GetMapping("/tree")
    @ApiOperation("获取全国省市区级联树")
    public ResponseResult<List<RegionTreeDto>> getRegionTree() {
        return ResponseResult.success(regionLocalService.getRegionTree());
    }
}