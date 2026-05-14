package me.zhengjie.modules.maint.domain.dto;

import lombok.Data;

import java.util.Date;

/**
 * 气瓶流转记录DTO
 * @author koma at cylinder-backend
 * @since 2026/5/14
 */
@Data
public class CylinderFlowRecordDto {

    private Long id;

    private String batchFlowNo;

    private String fromCompanyName;

    private String toCompanyName;

    private String flowTypeName;

    private String operatorName;

    private String remark;

    private Date createTime;
}
