package me.zhengjie.modules.maint.rest.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;

@Data
@Schema(description = "管理端：气瓶列表分页查询条件")
public class CylinderQueryReq extends PageQueryReq{

    @Schema(description = "气瓶唯一编码")
    private String code;

    @Schema(description = "气瓶状态 (0建档 1在库 2流转 3待检 4报废)")
    private Integer currentStatus;

    @Schema(description = "当前所属企业ID (超级管理员用来筛选企业)")
    private Long currentCompanyId;

    @Schema(description = "制造商企业ID")
    private Long manufacturerId;

    @Schema(description = "气瓶规格 (如: WP050)")
    private String spec;

    @Schema(description = "下次年检日期-开始 (格式: yyyy-MM-dd)")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date nextInspectDateStart;

    @Schema(description = "下次年检日期-结束 (格式: yyyy-MM-dd)")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date nextInspectDateEnd;
}