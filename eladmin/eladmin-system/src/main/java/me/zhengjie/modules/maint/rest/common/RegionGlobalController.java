package me.zhengjie.modules.maint.rest.common;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import me.zhengjie.annotation.rest.AnonymousAccess;
import me.zhengjie.modules.maint.domain.cylinder.RegionGlobalService;
import me.zhengjie.modules.maint.domain.dto.RegionTreeDto;
import me.zhengjie.sys.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Api(value = "通用：全球地区接口")
@RestController
@RequestMapping("/api/common/region")
@RequiredArgsConstructor
public class RegionGlobalController {
    
    private final RegionGlobalService regionService;
    
    @GetMapping("/countries")
    @ApiOperation("获取国家列表")
    @AnonymousAccess
    public ResponseResult<List<RegionTreeDto>> countries(
            @RequestParam(defaultValue = "zh-CN") String lang) {
        return ResponseResult.success(regionService.getCountries(lang));
    }
    
    @GetMapping("/states")
    @ApiOperation("根据国家获取州/省")
    @AnonymousAccess
    public ResponseResult<List<RegionTreeDto>> states(
            @RequestParam Integer countryId,
            @RequestParam(defaultValue = "zh-CN") String lang) {
        return ResponseResult.success(regionService.getStates(countryId, lang));
    }
    
    @GetMapping("/cities")
    @ApiOperation("根据州获取城市")
    @AnonymousAccess
    public ResponseResult<List<RegionTreeDto>> cities(
            @RequestParam Integer stateId,
            @RequestParam(defaultValue = "zh-CN") String lang) {
        return ResponseResult.success(regionService.getCities(stateId, lang));
    }
}