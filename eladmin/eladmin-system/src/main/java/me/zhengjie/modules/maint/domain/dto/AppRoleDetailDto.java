package me.zhengjie.modules.maint.domain.dto;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppPermission;

import java.util.Date;
import java.util.List;

@Data
@Schema(description = "APP角色详情(含权限)返回实体")
public class AppRoleDetailDto {
    
    @Schema(description = "角色ID")
    private Long id;
    
    @Schema(description = "角色名称")
    private String name;
    
    @Schema(description = "企业ID")
    private Long companyId;
    
    @Schema(description = "创建时间")
    private Date createTime;
    
    @Schema(description = "该角色拥有的完整权限列表")
    private List<AppPermission> permissions;
    
    // 如果前端画“权限树”只需要 ID 数组（用于打勾），你也可以多返回一个这个：
    @Schema(description = "拥有的权限ID集合(供前端快速回显打勾)")
    private List<Long> permissionIds; 
}