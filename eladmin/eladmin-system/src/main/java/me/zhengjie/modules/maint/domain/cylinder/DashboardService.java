package me.zhengjie.modules.maint.domain.cylinder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.entity.CompanyDailyStats;
import me.zhengjie.modules.maint.domain.cylinder.entity.CylinderDistributionStats;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CompanyDailyStatsMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CylinderDistributionStatsMapper;
import me.zhengjie.modules.maint.domain.dto.DashboardDto;
import me.zhengjie.modules.maint.domain.dto.DashboardQueryDto;
import me.zhengjie.modules.maint.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CompanyDailyStatsMapper dailyStatsMapper;
    private final CylinderDistributionStatsMapper distributionStatsMapper;
    
    /**
     * ==========================================
     * 接口一：获取动态天数充装量趋势 (动态补零算法)
     * ==========================================
     */
    public List<DashboardDto.TrendItemDto> getDynamicFillTrend(int days) {
        Long myCompanyId = SecurityUtils.getCompanyId();
        Boolean isAdmin = SecurityUtils.getCurrentUser().getUser().getIsAdmin();
        
        Date today = new Date();
        // 计算起始日期。如果查询 7 天，起始日是前 6 天。
        Date startDate = DateUtil.offsetDay(today, -(days - 1));
        
        // 1. 数据库聚合查出时间窗口内的零散业绩
        QueryWrapper<CompanyDailyStats> query = new QueryWrapper<>();
        query.select("stat_date as statDate", "SUM(fill_count) as fillCount")
             .ge("stat_date", DateUtil.beginOfDay(startDate))
             .le("stat_date", DateUtil.endOfDay(today));
        
        if (!isAdmin) {
            query.eq("company_id", myCompanyId);
        }
        query.groupBy("stat_date").orderByAsc("stat_date");
        
        List<CompanyDailyStats> dbStats = dailyStatsMapper.selectList(query);
        
        // 2. 转为极速查找的 Map (MM-dd -> 数量)
        Map<String, Integer> dataMap = new HashMap<>();
        if (CollUtil.isNotEmpty(dbStats)) {
            dataMap = dbStats.stream().collect(Collectors.toMap(
                    stat -> DateUtil.format(stat.getStatDate(), "MM-dd"),
                    stat -> stat.getFillCount() != null ? stat.getFillCount() : 0
            ));
        }
        
        // 3. 动态连续日期补零算法
        List<DashboardDto.TrendItemDto> resultList = new ArrayList<>();
        
        for (int i = days - 1; i >= 0; i--) {
            DateTime currentDay = DateUtil.offsetDay(today, -i);
            String dayStr = DateUtil.format(currentDay, "MM-dd");
            
            DashboardDto.TrendItemDto item = new DashboardDto.TrendItemDto();
            item.setDate(dayStr);
            item.setValue(dataMap.getOrDefault(dayStr, 0));
            
            resultList.add(item);
        }
        
        return resultList;
    }
    
    /**
     * ==========================================
     * 接口二：获取全国/全省气瓶分布热力图 (支持动态排序与 TOP N)
     * ==========================================
     */
    public List<DashboardDto.MapChartDto> getDynamicRegionalDistributionMap(DashboardQueryDto.MapReq req) {
        Long myCompanyId = SecurityUtils.getCompanyId();
        Boolean isAdmin = SecurityUtils.getCurrentUser().getUser().getIsAdmin();
        
        QueryWrapper<CylinderDistributionStats> query = new QueryWrapper<>();
        query.select("province", "SUM(total_count) as totalCount");
        
        if (!isAdmin) {
            query.eq("company_id", myCompanyId);
        }
        
        query.groupBy("province");
        
        // 🚀 动态排序逻辑：按聚合后的数量进行排序
        if ("ASC".equalsIgnoreCase(req.getSort())) {
            query.orderByAsc("SUM(total_count)");
        } else {
            query.orderByDesc("SUM(total_count)");
        }
        
        // 🚀 动态 LIMIT 逻辑：如果前端传了 limit=10，这在 MySQL 层面能直接拦截，大幅降低内存消耗
        if (req.getLimit() != null && req.getLimit() > 0) {
            query.last("LIMIT " + req.getLimit());
        }
        
        List<CylinderDistributionStats> dbStats = distributionStatsMapper.selectList(query);
        
        List<DashboardDto.MapChartDto> result = new ArrayList<>();
        if (CollUtil.isNotEmpty(dbStats)) {
            for (CylinderDistributionStats stat : dbStats) {
                if (stat.getProvince() == null) continue;
                
                DashboardDto.MapChartDto dto = new DashboardDto.MapChartDto();
                // 清洗省份名称，适配 ECharts
                String cleanName = stat.getProvince()
                                       .replace("省", "")
                                       .replace("市", "")
                                       .replace("自治区", "")
                                       .replace("回族", "")
                                       .replace("维吾尔", "")
                                       .replace("壮族", "");
                
                dto.setName(cleanName);
                // MyBatis-Plus 返回的 SUM 别名映射到 totalCount
                dto.setValue(stat.getTotalCount() != null ? stat.getTotalCount() : 0);
                
                result.add(dto);
            }
        }
        return result;
    }
}