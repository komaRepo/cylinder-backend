package me.zhengjie.modules.maint.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "扫码记录分页返回对象")
public class ScanRecordPageDto {

    @Schema(description = "扫码记录ID")
    private Long id;

    @Schema(description = "气瓶ID")
    private Long cylinderId;

    @Schema(description = "气瓶编号")
    private String cylinderCode;

    @Schema(description = "扫码用户ID")
    private Long userId;

    @Schema(description = "扫码用户名称")
    private String username;

    @Schema(description = "扫码企业ID")
    private Long companyId;

    @Schema(description = "扫码类型 1查询 2出库 3入库 4充装 5年检")
    private Integer scanType;

    @Schema(description = "扫码时间")
    private Date scanTime;
}
