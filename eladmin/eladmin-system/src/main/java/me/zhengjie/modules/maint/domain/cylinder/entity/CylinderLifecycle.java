package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import me.zhengjie.modules.maint.domain.enums.EventType;

import java.util.Date;

@Data
@TableName("cylinder_lifecycle")
@Schema(description = "气瓶生命周期表")
public class CylinderLifecycle {
    @TableId(type = IdType.AUTO)
    @Schema(description = "生命周期ID")
    private Long id;
    
    @Schema(description = "气瓶ID")
    private Long cylinderId;
    
    @Schema(description = "事件类型 1生产 2销售 3流转 4充气 5年检 6维修 7报废")
    private Integer eventType;
    
    @Schema(description = "相关企业ID")
    private Long companyId;
    
    @Schema(description = "操作人ID")
    private Long operatorId;
    
    @Schema(description = "事件时间")
    private Date eventTime;
    
    @Schema(description = "备注")
    private String remark;
}