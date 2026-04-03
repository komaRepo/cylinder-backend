package me.zhengjie.modules.maint.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DashboardQueryDto {

    @Data
    @ApiModel("趋势折线图查询参数")
    public static class TrendReq {
        @ApiModelProperty(value = "查询天数(默认30天)", example = "7")
        private Integer days = 30; // 默认给30天
    }

    @Data
    @ApiModel("地区分布查询参数")
    public static class MapReq {
        @ApiModelProperty(value = "排序方式: DESC降序, ASC升序 (默认DESC)", example = "DESC")
        private String sort = "DESC";

        @ApiModelProperty(value = "限制返回前N条(用于TOP排行榜), 不传则返回全部", example = "10")
        private Integer limit;
    }
}