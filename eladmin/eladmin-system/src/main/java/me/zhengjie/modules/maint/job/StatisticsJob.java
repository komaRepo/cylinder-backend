package me.zhengjie.modules.maint.job;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.CompanyDailyStatsService;
import me.zhengjie.modules.maint.domain.cylinder.CylinderDistributionStatsService;
import me.zhengjie.modules.maint.domain.cylinder.entity.CompanyDailyStats;
import me.zhengjie.modules.maint.domain.cylinder.entity.CylinderDistributionStats;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CompanyDailyStatsMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CylinderDistributionStatsMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsJob {
    private static final Date TOTAL_STAT_DATE = DateUtil.parseDate("1970-01-01");

    private final CompanyDailyStatsMapper companyDailyStatsMapper;
    private final CompanyDailyStatsService companyDailyStatsService;
    
    private final CylinderDistributionStatsMapper distributionStatsMapper;
    private final CylinderDistributionStatsService distributionStatsService;

    /**
     * 每 10 分钟刷新一次大盘统计快照。
     * 只维护指标卡需要的 TOTAL/MONTH/TODAY 三类快照。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshDashboardStats() {
        log.info("========== 开始刷新 [大盘统计快照] ==========");
        long startMillis = System.currentTimeMillis();

        Date now = new Date();
        Date today = DateUtil.beginOfDay(now);
        Date monthStart = DateUtil.beginOfMonth(now);

        refreshStats(CompanyDailyStats.STAT_TYPE_TODAY, today, today, now);
        refreshStats(CompanyDailyStats.STAT_TYPE_MONTH, monthStart, monthStart, now);
        refreshStats(CompanyDailyStats.STAT_TYPE_TOTAL, TOTAL_STAT_DATE, null, now);

        log.info("========== [大盘统计快照] 刷新完成，耗时: {} ms ==========",
                System.currentTimeMillis() - startMillis);
    }

    private void refreshStats(Integer statType, Date statDate, Date startTime, Date endTime) {
        companyDailyStatsMapper.delete(new LambdaQueryWrapper<CompanyDailyStats>()
                .eq(CompanyDailyStats::getStatType, statType)
                .eq(CompanyDailyStats::getStatDate, statDate));

        List<CompanyDailyStats> statsList = companyDailyStatsMapper.aggregateStats(startTime, endTime, statDate, statType);
        if (!statsList.isEmpty()) {
            companyDailyStatsService.saveBatch(statsList, 1000);
        }
    }

    /**
     * 任务二：每天凌晨 2:00 执行，生成【全国气瓶分布大盘快照】
     * Cron 表达式: 0 0 2 * * ? (每天凌晨2点)
     */
    @Transactional(rollbackFor = Exception.class)
    public void generateDistributionSnapshot() {
        log.info("========== 开始执行 [全国气瓶分布快照] 定时任务 ==========");
        long startMillis = System.currentTimeMillis();

        // 1. 由于这是一张“快照表”（只看当前最新状态），先物理清空旧快照
        // 生产环境中，数据量极大时建议用 TRUNCATE TABLE，这里为了兼容 MyBatis-Plus 用全表 delete
        distributionStatsMapper.delete(null);

        // 2. 执行极速聚合 SQL
        List<CylinderDistributionStats> snapshotList = distributionStatsMapper.aggregateDistributionSnapshot();

        // 3. 批量写入全新快照
        if (!snapshotList.isEmpty()) {
            distributionStatsService.saveBatch(snapshotList, 2000);
        }

        log.info("========== [全国气瓶分布快照] 完成，耗时: {} ms，共生成 {} 个地区网点分布组合 ==========", 
                (System.currentTimeMillis() - startMillis), snapshotList.size());
    }
}
