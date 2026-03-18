package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import me.zhengjie.modules.maint.domain.enums.OperationType;
import me.zhengjie.modules.maint.domain.enums.TargetType;

import java.util.Date;

@Data
@TableName("operation_log")
@Schema(description = "操作日志表")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;
    
    @Schema(description = "操作用户ID")
    private Long userId;
    
    @Schema(description = "操作名称")
    private String operation;
    
    @Schema(description = "操作对象类型")
    private String targetType;
    
    @Schema(description = "对象ID")
    private Long targetId;
    
    @Schema(description = "IP地址")
    private String ip;
    
    @Schema(description = "操作时间(分区键)")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}