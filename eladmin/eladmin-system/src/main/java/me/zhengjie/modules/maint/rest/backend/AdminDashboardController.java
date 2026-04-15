package me.zhengjie.modules.maint.rest.backend;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.zhengjie.modules.maint.domain.cylinder.DashboardService;
import me.zhengjie.modules.maint.domain.dto.DashboardDataDto;
import me.zhengjie.modules.maint.domain.dto.DashboardDto;
import me.zhengjie.modules.maint.domain.dto.DashboardQueryDto;
import me.zhengjie.sys.ResponseResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "系统：仪表盘数据接口")
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "大屏可视化聚合接口")
public class AdminDashboardController {

    private final DashboardService dashboardService;
    
    
    @GetMapping("/cards")
    @ApiOperation("获取顶部核心指标卡片 (融合实时与离线计算)")
    public ResponseResult<DashboardDataDto.IndicatorCards> getDashboardCards() {
        return ResponseResult.success(dashboardService.getDashboardCards());
    }
    
    @GetMapping("/charts/status-pie")
    @ApiOperation("获取气瓶状态分布饼图")
    public ResponseResult<List<DashboardDataDto.StatusPieChart>> getStatusPieChart() {
        return ResponseResult.success(dashboardService.getStatusPieChart());
    }
    
    @GetMapping("/trend/fill")
    @ApiOperation("获取动态天数充气量趋势折线图")
    // @PreAuthorize("@el.check('dashboard:list')")
    public ResponseResult<List<DashboardDto.TrendItemDto>> getFillTrend(DashboardQueryDto.TrendReq req) {
        // 如果前端传了非法值(如负数或0)，自动纠正为默认 30 天
        int days = (req.getDays() != null && req.getDays() > 0) ? req.getDays() : 30;
        return ResponseResult.success(dashboardService.getDynamicFillTrend(days));
    }
    
    @GetMapping("/map/distribution")
    @ApiOperation("获取全国气瓶地区分布热力地图及排行榜")
    // @PreAuthorize("@el.check('dashboard:list')")
    public ResponseResult<List<DashboardDto.MapChartDto>> getDistributionMap(DashboardQueryDto.MapReq req) {
        return ResponseResult.success(dashboardService.getDynamicRegionalDistributionMap(req));
    }
    
    @GetMapping("/charts/ranking")
    @ApiOperation("获取企业加气业绩排行榜 TOP 5")
    public ResponseResult<List<DashboardDataDto.KpiRankDto>> getFillRanking() {
        return ResponseResult.success(dashboardService.getFillRanking());
    }
    
    @GetMapping("/charts/efficiency")
    @ApiOperation("获取资产效能/活跃度分布")
    public ResponseResult<List<DashboardDataDto.EfficiencyDto>> getAssetEfficiency() {
        return ResponseResult.success(dashboardService.getAssetEfficiency());
    }
    
}