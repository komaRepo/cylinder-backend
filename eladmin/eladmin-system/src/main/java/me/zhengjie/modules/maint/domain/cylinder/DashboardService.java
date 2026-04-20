package me.zhengjie.modules.maint.domain.cylinder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.entity.*;
import me.zhengjie.modules.maint.domain.cylinder.mapper.*;
import me.zhengjie.modules.maint.domain.dto.DashboardDataDto;
import me.zhengjie.modules.maint.domain.dto.DashboardDto;
import me.zhengjie.modules.maint.domain.dto.DashboardQueryDto;
import me.zhengjie.modules.maint.domain.enums.CylinderStatus;
import me.zhengjie.modules.maint.domain.enums.ScanType;
import me.zhengjie.modules.maint.util.SecurityUtils;
import me.zhengjie.modules.security.service.dto.JwtUserDto;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CompanyDailyStatsMapper dailyStatsMapper;
    private final CylinderDistributionStatsMapper distributionStatsMapper;
    private final RegionLocalService regionLocalService;
    private final CylinderMapper cylinderMapper;
    private final ScanRecordMapper scanRecordMapper;
    private final CompanyMapper companyMapper;
    
    /**
     * 🚀 【核心修复】：获取当前用户有权限查看的所有企业ID（本级 + 所有下级）
     * 通过在 SQL 中使用 IN (ids) 完美替代 eq(id)，实现层级数据穿透！
     */
    private List<Long> getAccessibleCompanyIds(JwtUserDto currentUser) {
        List<Long> ids = new ArrayList<>();
        Long myCompanyId = currentUser.getCompanyId();
        
        if (myCompanyId == null) return ids;
        ids.add(myCompanyId); // 加入本级
        
        // 利用企业路径(Path)查询所有子节点 (假设 Path 格式为 /0/1/2/)
        String path = currentUser.getCompanyPath();
        if (path != null && !path.isEmpty()) {
            QueryWrapper<Company> q = new QueryWrapper<>();
            q.select("id").likeRight("path", path); // LIKE '/0/1/2/%'
            List<Company> children = companyMapper.selectList(q);
            if (CollUtil.isNotEmpty(children)) {
                ids.addAll(children.stream().map(Company::getId).collect(Collectors.toList()));
            }
        }
        // 去重并返回
        return ids.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * ==========================================
     * 接口一：获取动态天数充装量趋势 (动态补零算法)
     * ==========================================
     */
    public DashboardDataDto.IndicatorCards getDashboardCards() {
        JwtUserDto currentUser = SecurityUtils.getCurrentUser();
        boolean isAdmin = currentUser.getUser().getIsAdmin();
        boolean isFiller = Boolean.TRUE.equals(currentUser.getTypeFiller());
        
        // 🔒 获取数据权限范围
        List<Long> accessibleIds = getAccessibleCompanyIds(currentUser);
        DashboardDataDto.IndicatorCards cards = new DashboardDataDto.IndicatorCards();
        
        // 1. 【通用指标】：状态统计
        QueryWrapper<Cylinder> statusQuery = new QueryWrapper<>();
        statusQuery.select("current_status as currentStatus", "COUNT(id) as id");
        if (!isAdmin && CollUtil.isNotEmpty(accessibleIds)) {
            statusQuery.in("current_company_id", accessibleIds); // 👈 替换为 IN
        }
        statusQuery.groupBy("current_status");
        
        List<Cylinder> statusCounts = cylinderMapper.selectList(statusQuery);
        int total = 0;
        if (CollUtil.isNotEmpty(statusCounts)) {
            for (Cylinder stat : statusCounts) {
                int count = stat.getId() != null ? stat.getId().intValue() : 0;
                total += count;
                if (stat.getCurrentStatus() != null) {
                    switch (stat.getCurrentStatus()) {
                        case IN_STOCK: cards.setInStockCount(count); break;
                        case TRANSIT: cards.setFlowingCount(count); break;
                        case WAIT_INSPECT: cards.setBrokenCount(count); break;
                        default: break;
                    }
                }
            }
        }
        cards.setTotalCount(total);
        
        // 2. 【临期提醒】
        QueryWrapper<Cylinder> expireQuery = new QueryWrapper<>();
        expireQuery.between("next_inspection_date", new Date(), DateUtil.offsetDay(new Date(), 30));
        if (!isAdmin && CollUtil.isNotEmpty(accessibleIds)) {
            expireQuery.in("current_company_id", accessibleIds); // 👈 替换为 IN
        }
        cards.setExpiringCount(Math.toIntExact(cylinderMapper.selectCount(expireQuery)));
        
        // 3. 【角色专属】：充气商 T+1 加气量融合
        if (isFiller || isAdmin) {
            Date todayStart = DateUtil.beginOfDay(new Date());
            QueryWrapper<ScanRecord> todayQuery = new QueryWrapper<>();
            todayQuery.eq("scan_type", ScanType.FILL.getCode()).ge("scan_time", todayStart);
            if (!isAdmin && CollUtil.isNotEmpty(accessibleIds)) {
                todayQuery.in("company_id", accessibleIds); // 👈 替换为 IN
            }
            int todayFill = Math.toIntExact(scanRecordMapper.selectCount(todayQuery));
            cards.setTodayFillCount(todayFill);
            
            Date monthStart = DateUtil.beginOfMonth(new Date());
            QueryWrapper<CompanyDailyStats> offlineQuery = new QueryWrapper<>();
            offlineQuery.select(
                    "SUM(fill_count) as fillCount",
                    "SUM(CASE WHEN stat_date >= '" + DateUtil.formatDate(monthStart) + "' THEN fill_count ELSE 0 END) as inCount"
            );
            if (!isAdmin && CollUtil.isNotEmpty(accessibleIds)) {
                offlineQuery.in("company_id", accessibleIds); // 👈 替换为 IN
            }
            CompanyDailyStats offlineResult = dailyStatsMapper.selectOne(offlineQuery);
            int historyFill = offlineResult != null && offlineResult.getFillCount() != null ? offlineResult.getFillCount() : 0;
            int monthOfflineFill = offlineResult != null && offlineResult.getInCount() != null ? offlineResult.getInCount() : 0;
            
            cards.setTotalFillCount(historyFill + todayFill);
            cards.setMonthFillCount(monthOfflineFill + todayFill);
        }
        
        // 4. 🚀 【高阶维度】：沉睡资产预警 (超30天未动)
        QueryWrapper<Cylinder> sleepQuery = new QueryWrapper<>();
        sleepQuery.in("current_status", CylinderStatus.IN_STOCK, CylinderStatus.TRANSIT)
                  .lt("last_fill_time", DateUtil.offsetDay(new Date(), -30));
        if (!isAdmin && CollUtil.isNotEmpty(accessibleIds)) {
            sleepQuery.in("current_company_id", accessibleIds); // 👈 替换为 IN
        }
        cards.setSleepingCount(Math.toIntExact(cylinderMapper.selectCount(sleepQuery)));
        
        // 5. 🚀 【高阶维度】：严重合规风险预警 (过期1年以上未报废)
        QueryWrapper<Cylinder> riskQuery = new QueryWrapper<>();
        riskQuery.ne("current_status", CylinderStatus.SCRAP)
                 .lt("next_inspection_date", DateUtil.offsetDay(new Date(), -365));
        if (!isAdmin && CollUtil.isNotEmpty(accessibleIds)) {
            riskQuery.in("current_company_id", accessibleIds); // 👈 替换为 IN
        }
        cards.setCriticalOverdueCount(Math.toIntExact(cylinderMapper.selectCount(riskQuery)));
        
        return cards;
    }
    
    /**
     * ==========================================
     * 接口二：获取状态分布饼图
     * ==========================================
     */
    public List<DashboardDataDto.StatusPieChart> getStatusPieChart() {
        JwtUserDto currentUser = SecurityUtils.getCurrentUser();
        boolean isAdmin = currentUser.getUser().getIsAdmin();
        List<Long> accessibleIds = getAccessibleCompanyIds(currentUser);
        
        QueryWrapper<Cylinder> query = new QueryWrapper<>();
        query.select("current_status as currentStatus", "COUNT(id) as id");
        if (!isAdmin && CollUtil.isNotEmpty(accessibleIds)) {
            query.in("current_company_id", accessibleIds); // 👈 替换为 IN
        }
        query.groupBy("current_status");
        
        List<Cylinder> statusCounts = cylinderMapper.selectList(query);
        List<DashboardDataDto.StatusPieChart> pieList = new ArrayList<>();
        
        if (CollUtil.isNotEmpty(statusCounts)) {
            for (Cylinder stat : statusCounts) {
                if (stat.getCurrentStatus() == null) continue;
                DashboardDataDto.StatusPieChart pie = new DashboardDataDto.StatusPieChart();
                pie.setValue(stat.getId() != null ? stat.getId().intValue() : 0);
                pie.setName(stat.getCurrentStatus().getName());
                pieList.add(pie);
            }
        }
        return pieList;
    }
    
    /**
     * ==========================================
     * 接口三：KPI 业绩龙虎榜 (本月充气量排行 TOP 5)
     * ==========================================
     */
    public List<DashboardDataDto.KpiRankDto> getFillRanking() {
        JwtUserDto currentUser = SecurityUtils.getCurrentUser();
        boolean isAdmin = currentUser.getUser().getIsAdmin();
        List<Long> accessibleIds = getAccessibleCompanyIds(currentUser);
        
        QueryWrapper<CompanyDailyStats> query = new QueryWrapper<>();
        query.select("company_id as companyId", "SUM(fill_count) as fillCount")
             .ge("stat_date", DateUtil.beginOfMonth(new Date()))
             .groupBy("company_id")
             .orderByDesc("SUM(fill_count)")
             .last("LIMIT 5");
        
        if (!isAdmin && CollUtil.isNotEmpty(accessibleIds)) {
            query.in("company_id", accessibleIds); // 👈 限定只对本级及下级网点进行排行
        }
        
        List<CompanyDailyStats> rankData = dailyStatsMapper.selectList(query);
        List<DashboardDataDto.KpiRankDto> result = new ArrayList<>();
        
        if (CollUtil.isNotEmpty(rankData)) {
            Set<Long> cIds = rankData.stream().map(CompanyDailyStats::getCompanyId).collect(Collectors.toSet());
            Map<Long, String> nameMap = companyMapper.selectBatchIds(cIds).stream()
                                                     .collect(Collectors.toMap(Company::getId, Company::getName));
            
            for (CompanyDailyStats record : rankData) {
                DashboardDataDto.KpiRankDto dto = new DashboardDataDto.KpiRankDto();
                dto.setCompanyName(nameMap.getOrDefault(record.getCompanyId(), "未知网点"));
                dto.setValue(record.getFillCount());
                result.add(dto);
            }
        }
        return result;
    }
    
    /**
     * ==========================================
     * 接口四：资产效能漏斗 (活跃度分布)
     * ==========================================
     */
    public List<DashboardDataDto.EfficiencyDto> getAssetEfficiency() {
        JwtUserDto currentUser = SecurityUtils.getCurrentUser();
        boolean isAdmin = currentUser.getUser().getIsAdmin();
        List<Long> accessibleIds = getAccessibleCompanyIds(currentUser);
        
        Date now = new Date();
        Date d7 = DateUtil.offsetDay(now, -7);
        Date d30 = DateUtil.offsetDay(now, -30);
        
        List<DashboardDataDto.EfficiencyDto> list = new ArrayList<>();
        list.add(new DashboardDataDto.EfficiencyDto("一周内活跃", countByFillTime(d7, now, accessibleIds, isAdmin)));
        list.add(new DashboardDataDto.EfficiencyDto("一个月内流转", countByFillTime(d30, d7, accessibleIds, isAdmin)));
        list.add(new DashboardDataDto.EfficiencyDto("沉睡资产", countByFillTime(null, d30, accessibleIds, isAdmin)));
        return list;
    }
    
    private Integer countByFillTime(Date start, Date end, List<Long> accessibleIds, boolean isAdmin) {
        QueryWrapper<Cylinder> q = new QueryWrapper<>();
        if (start != null) q.ge("last_fill_time", start);
        if (end != null) q.le("last_fill_time", end);
        if (!isAdmin && CollUtil.isNotEmpty(accessibleIds)) {
            q.in("current_company_id", accessibleIds); // 👈 替换为 IN
        }
        q.ne("current_status", CylinderStatus.SCRAP);
        return Math.toIntExact(cylinderMapper.selectCount(q));
    }
    
    /**
     * ==========================================
     * 接口五：获取近期加气趋势折线图
     * ==========================================
     */
    public List<DashboardDto.TrendItemDto> getDynamicFillTrend(int days) {
        JwtUserDto currentUser = SecurityUtils.getCurrentUser();
        boolean isAdmin = currentUser.getUser().getIsAdmin();
        List<Long> accessibleIds = getAccessibleCompanyIds(currentUser);
        
        Date today = new Date();
        Date startDate = DateUtil.offsetDay(today, -(days - 1));
        
        QueryWrapper<CompanyDailyStats> query = new QueryWrapper<>();
        query.select("stat_date as statDate", "SUM(fill_count) as fillCount")
             .ge("stat_date", DateUtil.beginOfDay(startDate))
             .le("stat_date", DateUtil.endOfDay(today));
        
        if (!isAdmin && CollUtil.isNotEmpty(accessibleIds)) {
            query.in("company_id", accessibleIds); // 👈 替换为 IN
        }
        query.groupBy("stat_date").orderByAsc("stat_date");
        
        List<CompanyDailyStats> dbStats = dailyStatsMapper.selectList(query);
        Map<String, Integer> dataMap = new HashMap<>();
        if (CollUtil.isNotEmpty(dbStats)) {
            dataMap = dbStats.stream().collect(Collectors.toMap(
                    stat -> DateUtil.format(stat.getStatDate(), "MM-dd"),
                    stat -> stat.getFillCount() != null ? stat.getFillCount() : 0
            ));
        }
        
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
     * 接口六：获取全国/全球气瓶地区分布热力地图
     * ==========================================
     */
    public List<DashboardDto.MapChartDto> getDynamicRegionalDistributionMap(DashboardQueryDto.MapReq req) {
        JwtUserDto currentUser = SecurityUtils.getCurrentUser();
        boolean isAdmin = currentUser.getUser().getIsAdmin();
        // 🔒 依然遵循你要求的权限维度：只能看本级和下级
        List<Long> accessibleIds = getAccessibleCompanyIds(currentUser);
        
        // 1. 在气瓶主表中统计各企业“在库”气瓶数量
        QueryWrapper<Cylinder> query = new QueryWrapper<>();
        query.select("current_company_id as currentCompanyId", "COUNT(id) as id") // 借用 id 字段接收 count 值
             .eq("current_status", CylinderStatus.IN_STOCK); // 👈 只统计在库状态
        
        if (!isAdmin && CollUtil.isNotEmpty(accessibleIds)) {
            query.in("current_company_id", accessibleIds);
        }
        
        query.groupBy("current_company_id");
        
        // 排序逻辑调整为按气瓶数量排序
        if ("ASC".equalsIgnoreCase(req.getSort())) {
            query.orderByAsc("COUNT(id)");
        } else {
            query.orderByDesc("COUNT(id)");
        }
        
        if (req.getLimit() != null && req.getLimit() > 0) {
            query.last("LIMIT " + req.getLimit());
        }
        
        // 执行统计查询
        List<Cylinder> stats = cylinderMapper.selectList(query);
        List<DashboardDto.MapChartDto> result = new ArrayList<>();
        
        if (CollUtil.isNotEmpty(stats)) {
            // 2. 批量提取涉及到的企业 ID，准备翻译名称和坐标
            Set<Long> companyIds = stats.stream()
                                        .map(Cylinder::getCurrentCompanyId)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toSet());
            
            // 3. 一次性查询企业档案中的坐标和名称
            Map<Long, Company> companyMap = new HashMap<>();
            if (CollUtil.isNotEmpty(companyIds)) {
                List<Company> companies = companyMapper.selectList(
                        new LambdaQueryWrapper<Company>()
                                .in(Company::getId, companyIds)
                                .select(Company::getId, Company::getName, Company::getLongitude, Company::getLatitude)
                );
                companyMap = companies.stream().collect(Collectors.toMap(Company::getId, c -> c));
            }
            
            // 4. 组装最终返回给前端的地图点位数据
            for (Cylinder stat : stats) {
                Company comp = companyMap.get(stat.getCurrentCompanyId());
                if (comp == null || comp.getLongitude() == null) continue; // 没坐标的企业不显示在地图上
                
                DashboardDto.MapChartDto dto = new DashboardDto.MapChartDto();
                dto.setName(comp.getName());
                dto.setCode(comp.getId().toString());
                dto.setValue(stat.getId().intValue()); // 这里的 id 存的是 count(*) 的结果
                dto.setLng(comp.getLongitude());
                dto.setLat(comp.getLatitude());
                
                result.add(dto);
            }
        }
        
        return result;
    }
    
}