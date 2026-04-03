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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsJob {

    private final CompanyDailyStatsMapper companyDailyStatsMapper;
    private final CompanyDailyStatsService companyDailyStatsService;
    
    private final CylinderDistributionStatsMapper distributionStatsMapper;
    private final CylinderDistributionStatsService distributionStatsService;

    /**
     * 任务一：每天凌晨 1:00 执行，统计【昨天】各企业的业务量
     * Cron 表达式: 0 0 1 * * ? (每天凌晨1点)
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void generateDailyStats() {
        log.info("========== 开始执行 [企业每日业务统计] 定时任务 ==========");
        long startMillis = System.currentTimeMillis();

        // 1. 确定时间窗口：昨天的 00:00:00 到 23:59:59
        Date yesterday = DateUtil.yesterday();
        Date startTime = DateUtil.beginOfDay(yesterday);
        Date endTime = DateUtil.endOfDay(yesterday);

        // 2. 防重执行：如果因为服务器重启导致重复跑，先把今天算过的数据删掉
        companyDailyStatsMapper.delete(new LambdaQueryWrapper<CompanyDailyStats>()
                .eq(CompanyDailyStats::getStatDate, DateUtil.parseDate(DateUtil.formatDate(yesterday))));

        // 3. 执行极速 SQL 聚合
        List<CompanyDailyStats> statsList = companyDailyStatsMapper.aggregateDailyStats(startTime, endTime, yesterday);

        // 4. 批量落库
        if (!statsList.isEmpty()) {
            companyDailyStatsService.saveBatch(statsList, 1000);
        }

        log.info("========== [企业每日业务统计] 完成，耗时: {} ms，共生成 {} 条记录 ==========", 
                (System.currentTimeMillis() - startMillis), statsList.size());
    }

    /**
     * 任务二：每天凌晨 2:00 执行，生成【全国气瓶分布大盘快照】
     * Cron 表达式: 0 0 2 * * ? (每天凌晨2点)
     */
    @Scheduled(cron = "0 0 2 * * ?")
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