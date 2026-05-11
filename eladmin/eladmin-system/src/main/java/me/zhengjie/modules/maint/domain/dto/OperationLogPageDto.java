package me.zhengjie.modules.maint.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import me.zhengjie.modules.maint.domain.enums.OperationType;
import me.zhengjie.modules.maint.domain.enums.TargetType;

import java.util.Date;

@Data
@Schema(description = "操作日志分页返回对象")
public class OperationLogPageDto {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作用户ID")
    private Long userId;

    @Schema(description = "操作用户名")
    private String username;

    @Schema(description = "操作名称")
    private OperationType operation;

    @Schema(description = "操作名称中文")
    private String operationName;

    @Schema(description = "操作对象类型")
    private TargetType targetType;

    @Schema(description = "操作对象类型中文")
    private String targetTypeName;

    @Schema(description = "对象ID")
    private Long targetId;

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "操作时间")
    private Date createTime;

    @Schema(description = "流转记录ID")
    private Long flowId;

    @Schema(description = "关联气瓶信息")
    private CylinderInfo cylinderInfo;

    @Schema(description = "关联企业信息")
    private CompanyInfo companyInfo;

    @Data
    public static class CylinderInfo {
        private Long id;
        private String code; // 气瓶编号
        private String spec; // 规格
        private Double volume; // 容积
        private String fromCompanyName; // 来源企业名称
        private String toCompanyName; // 目标企业名称
    }

    @Data
    public static class CompanyInfo {
        private Long id;
        private String name; // 企业名称
        private String code; // 企业代码
    }
}
