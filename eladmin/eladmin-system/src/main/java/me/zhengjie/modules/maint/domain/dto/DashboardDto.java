package me.zhengjie.modules.maint.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 仪表盘可视化传输对象大宽表
 */
@Data
public class DashboardDto {

    @Data
    @Schema(description = "趋势图表单点数据 (对象数组模式)")
    public static class TrendItemDto {
        @Schema(description = "连续日期 (如: 03-01)")
        private String date;

        @Schema(description = "当日业务数值 (如充气量)")
        private Integer value;
        
        // 🚀 预留扩展点：未来如果加折线，直接在这里加字段即可
        // private Integer inspectionCount; 
        // private Integer outCount;
    }

    @Data
    @Schema(description = "全国/全省热力地图标准结构")
    public static class MapChartDto {
        @Schema(description = "区域名称 (如: '广东', '北京')")
        private String name;
        
        @Schema(description = "区域代码 (如: '440000' 代表广东省)")
        private String code;

        @Schema(description = "分布数量")
        private Integer value;
    }
}