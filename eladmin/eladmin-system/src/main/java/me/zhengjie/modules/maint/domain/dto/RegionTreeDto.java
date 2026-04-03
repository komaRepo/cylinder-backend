package me.zhengjie.modules.maint.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "省市区树形节点")
// 极其重要：如果 children 为空（比如到了区县级），不要序列化给前端，防止 Cascader 出现空白下一级！
@JsonInclude(JsonInclude.Include.NON_EMPTY) 
public class RegionTreeDto {

    @Schema(description = "节点的值 (行政代码)")
    private String value;

    @Schema(description = "节点的显示文本 (区域名称)")
    private String label;

    @Schema(description = "子节点列表")
    private List<RegionTreeDto> children;
}