package me.zhengjie.modules.maint.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import me.zhengjie.modules.maint.domain.enums.CylinderStatus;

import java.util.Date;
import java.util.List;

@Data
@Schema(description = "管理端：气瓶全维度溯源详情")
public class CylinderDetailDto {

    // ================= 1. 物理档案基础信息 =================
    @Schema(description = "气瓶ID")
    private Long id;
    
    @Schema(description = "气瓶唯一编号")
    private String code;
    
    @Schema(description = "气瓶规格")
    private String spec;
    
    @Schema(description = "容积(L)")
    private Double volume;
    
    @Schema(description = "气瓶当前状态")
    private CylinderStatus currentStatus;
    
    @Schema(description = "制造日期")
    private Date manufactureDate;
    
    @Schema(description = "下次年检日期")
    private Date nextInspectionDate;

    // ================= 2. 翻译后的归属信息 =================
    @Schema(description = "制造商名称")
    private String manufacturerName;
    
    @Schema(description = "当前所属企业名称")
    private String currentCompanyName;
    
    @Schema(description = "生产批次号")
    private String batchNo;

    // ================= 3. 核心：生命周期时间轴 =================
    @Schema(description = "生命周期轨迹时间轴 (按时间倒序)")
    private List<LifecycleNode> timeline;

    /**
     * 时间轴节点内部类
     */
    @Data
    @Schema(description = "时间轴节点")
    public static class LifecycleNode {
        @Schema(description = "事件类型枚举的名称 (如: 扫码充气, 扫码出库)")
        private String eventName;
        
        @Schema(description = "发生时间")
        private Date eventTime;
        
        @Schema(description = "发生地(操作网点/企业名称)")
        private String companyName;
        
        @Schema(description = "操作人姓名")
        private String operatorName;
        
        @Schema(description = "业务备注")
        private String remark;
    }
}