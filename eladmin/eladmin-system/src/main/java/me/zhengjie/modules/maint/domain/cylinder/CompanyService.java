/*
 * Copyright 2026 The rfid-backend Project under the WTFPL License,
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
package me.zhengjie.modules.maint.domain.cylinder;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CompanyMapper;
import me.zhengjie.modules.maint.domain.enums.CompanyStatus;
import me.zhengjie.modules.maint.domain.enums.CompanyType;
import org.springframework.stereotype.Service;

/**
 * 企业服务类
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService extends ServiceImpl<CompanyMapper, Company> {
    
    
    /**
     * 管理后台企业注册
     */
    public void register(CompanyType type, String name, String creditCode, String legalName,
            String legalCode, String contactName, String contactPhone, String province,
            String city, String district, String address, Long parentId, String businessLicense,
            String dangerBusinessLicense, String cylinderFillLicense, String specialEquipmentLicense) {
        //参数校验
        _checkParam(type, name, creditCode, legalName, legalCode, province, city, district, businessLicense, dangerBusinessLicense, cylinderFillLicense, specialEquipmentLicense);
        //注册企业
        Company company = new Company();
        company.setName(name);
        company.setCode(creditCode);
        company.setLegalName(legalName);
        company.setLegalCode(legalCode);
        company.setContact(contactName);
        company.setPhone(contactPhone);
        company.setProvince(province);
        company.setCity(city);
        company.setDistrict(district);
        company.setAddress(address);
        company.setParentId(parentId);
        company.setStatus(CompanyStatus.INACTIVE);
        
        this.baseMapper.insert(company);
        
        //如果上层机构不为空，则查询上层机构信息
        String path = null;
        if (ObjectUtil.isNotEmpty(parentId)) {
            Company parent = this.baseMapper.selectById(parentId);
            path = parent.getPath().concat(company.getId().toString()).concat(",");
        } else {
            path = company.getId().toString().concat(",");
        }
        
        //更新路径信息
        company.setPath(path);
        this.baseMapper.updateById(company);
        
    }
    
    
    /**
     * 企业申请前置参数校验
     */
    private void _checkParam(CompanyType type, String name, String creditCode, String legalName, String legalCode,
            String province, String city, String district, String businessLicense, String dangerBusinessLicense,
            String cylinderFillLicense, String specialEquipmentLicense) {
        //todo 待校验企业四要素
        
        //todo 待校验自治区要填写市，直辖市要填写区县
        
        if (ObjectUtil.equals(type, CompanyType.DISTRIBUTOR)) {
                //分销商必须提供营业执照和危险化学品经营许可证
                if (ObjectUtil.hasEmpty(businessLicense, dangerBusinessLicense)) {
                    throw new IllegalArgumentException("分销商必须提供营业执照和危险化学品经营许可证");
                }
        } else if (ObjectUtil.equals(type, CompanyType.RETAILER)) {
                //加气站必须提供营业执照、危险化学品经营许可证、压力容器充装许可证和特种设备许可证
                if (ObjectUtil.hasEmpty(businessLicense, dangerBusinessLicense, cylinderFillLicense, specialEquipmentLicense)) {
                    throw new IllegalArgumentException("加气站必须提供营业执照、危险化学品经营许可证、压力容器充装许可证和特种设备许可证");
                }
        }
        
    }
}
