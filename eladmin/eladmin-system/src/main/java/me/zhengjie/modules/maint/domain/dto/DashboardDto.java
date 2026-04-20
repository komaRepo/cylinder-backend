package me.zhengjie.modules.maint.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

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
    @Schema(description = "地图企业分布点位 DTO")
    public static class MapChartDto {
        @Schema(description = "企业名称")
        private String name;
        
        @Schema(description = "企业 ID (作为唯一标识)")
        private String code;
        
        @Schema(description = "在库气瓶数量")
        private Integer value;
        
        @Schema(description = "经度")
        private BigDecimal lng;
        
        @Schema(description = "纬度")
        private BigDecimal lat;
    }
}