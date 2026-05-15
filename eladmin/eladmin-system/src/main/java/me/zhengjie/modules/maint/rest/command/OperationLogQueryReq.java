package me.zhengjie.modules.maint.rest.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.zhengjie.modules.maint.domain.enums.OperationType;
import me.zhengjie.modules.maint.domain.enums.TargetType;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作日志分页查询条件")
public class OperationLogQueryReq extends PageQueryReq {

    @Schema(description = "操作类型")
    private OperationType operation;

    @Schema(description = "操作对象类型")
    private TargetType targetType;

    @Schema(description = "对象ID")
    private Long targetId;

    @Schema(description = "气瓶二维码（仅当targetType为CYLINDER时生效）")
    private String qrcode;

    @Schema(description = "操作用户ID")
    private Long userId;

    @Schema(description = "IP地址")
    private String ip;

    @Schema(description = "开始时间 (格式: yyyy-MM-dd HH:mm:ss)")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @Schema(description = "结束时间 (格式: yyyy-MM-dd HH:mm:ss)")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
