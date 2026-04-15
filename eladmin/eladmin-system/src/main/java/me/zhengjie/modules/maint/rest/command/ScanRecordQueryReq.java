package me.zhengjie.modules.maint.rest.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "扫码记录分页查询条件")
public class ScanRecordQueryReq extends PageQueryReq {

    @Schema(description = "扫码类型 1查询 2出库 3入库 4充装 5年检")
    private Integer scanType;

    @Schema(description = "气瓶编号(支持模糊匹配)")
    private String cylinderCode;

    @Schema(description = "扫码用户ID")
    private Long userId;

    @Schema(description = "开始时间 (格式: yyyy-MM-dd HH:mm:ss)")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @Schema(description = "结束时间 (格式: yyyy-MM-dd HH:mm:ss)")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
