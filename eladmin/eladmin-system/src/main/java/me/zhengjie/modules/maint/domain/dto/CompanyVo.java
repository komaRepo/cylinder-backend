/*
 * Copyright 2026 The cylinder-backend Project under the WTFPL License,
 *
 *     http://www.wtfpl.net/about/
 *
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 * 代码千万行，注释第一行，编程不规范，日后泪两行
 *
 */
package me.zhengjie.modules.maint.domain.dto;

import lombok.Data;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * TODO
 *
 * @author koma at cylinder-backend
 * @since 2026/3/26
 */
@Data
public class CompanyVo {
    
    /** 企业ID */
    private Long id;
    /** 企业名称 */
    private String name;
    /** 企业代码 */
    private String code;
    
    @Mapper
    public interface Converter {
        
        Converter INSTANCE = Mappers.getMapper(Converter.class);
        
        CompanyVo fromEntity(Company company);
        
        List<CompanyVo> fromEntityList(List<Company> companies);
        
    }
    
}
