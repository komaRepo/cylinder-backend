package me.zhengjie.modules.maint.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class DashboardDataDto {

    @Data
    @Schema(description = "大盘顶部：核心指标卡片")
    public static class IndicatorCards {
        // ================= 1. 所有角色通用的气瓶资产指标 =================
        @Schema(description = "气瓶总数")
        private Integer totalCount = 0;
        
        @Schema(description = "在库气瓶数量 (status=1)")
        private Integer inStockCount = 0;
        
        @Schema(description = "出库/流转中气瓶数量 (status=2)")
        private Integer flowingCount = 0;
        
        @Schema(description = "故障/待检气瓶数量 (status=3)")
        private Integer brokenCount = 0;
        
        @Schema(description = "即将过期气瓶提醒 (30天内到期)")
        private Integer expiringCount = 0;

        // ================= 2. 充气商(Filler)特有的加气指标 =================
        @Schema(description = "历史加气总次数")
        private Integer totalFillCount;
        
        @Schema(description = "本月加气次数")
        private Integer monthFillCount;
        
        @Schema(description = "当日加气次数")
        private Integer todayFillCount;
        
        // --- 🚀 新增：资产效率与合规告警指标 ---
        @Schema(description = "沉睡资产数量 (超过30天未充气/未流转)")
        private Integer sleepingCount = 0;
        
        @Schema(description = "严重超期未报废告警 (过期1年以上且非报废状态)")
        private Integer criticalOverdueCount = 0;
    }

    @Data
    @Schema(description = "大盘图表：状态饼图")
    public static class StatusPieChart {
        @Schema(description = "状态名称 (如: 在库, 流转)")
        private String name;
        @Schema(description = "数量")
        private Integer value;
    }
    
    @Data
    @Schema(description = "KPI 排行榜节点")
    public static class KpiRankDto {
        @Schema(description = "企业/网点名称")
        private String companyName;
        @Schema(description = "统计数值")
        private Integer value;
    }
    
    @Data
    @Schema(description = "资产效率分布")
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EfficiencyDto {
        @Schema(description = "标签(如: 0-7天活跃, 30天以上未动)")
        private String label;
        @Schema(description = "占比/数量")
        private Integer value;
    }
    
    
}