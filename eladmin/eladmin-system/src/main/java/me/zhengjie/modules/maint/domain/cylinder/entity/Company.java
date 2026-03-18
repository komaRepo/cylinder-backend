package me.zhengjie.modules.maint.domain.cylinder.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import me.zhengjie.modules.maint.domain.enums.CompanyStatus;

import java.util.Date;

@Data
@TableName("company")
@Schema(description = "企业表")
public class Company {
    @TableId(type = IdType.AUTO)
    @Schema(description = "企业ID")
    private Long id;
    
    @Schema(description = "企业名称")
    private String name;
    
    @Schema(description = "企业编号")
    private String code;
    
    @Schema(description = "是否制造商 0否 1是")
    private Integer typeManufacturer;
    
    @Schema(description = "是否经销商 0否 1是")
    private Integer typeDealer;
    
    @Schema(description = "是否充气商 0否 1是")
    private Integer typeFiller;
    
    @Schema(description = "是否年检机构 0否 1是")
    private Integer typeInspection;
    
    @Schema(description = "上级经销商ID")
    private Long parentId;
    
    @Schema(description = "层级路径, 例如: 0,1,5,")
    private String path;
    
    @Schema(description = "省")
    private String province;
    
    @Schema(description = "市")
    private String city;
    
    @Schema(description = "区")
    private String district;
    
    @Schema(description = "详细地址")
    private String address;
    
    @Schema(description = "联系人")
    private String contact;
    
    @Schema(description = "联系电话")
    private String phone;
    
    @Schema(description = "状态 0禁用 1正常")
    private Integer status;
    
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}