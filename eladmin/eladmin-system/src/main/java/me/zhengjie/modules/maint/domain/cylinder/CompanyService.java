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
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CompanyMapper;
import me.zhengjie.modules.maint.domain.enums.CompanyStatus;
import me.zhengjie.modules.maint.domain.enums.CompanyType;
import me.zhengjie.modules.maint.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
    
    
    /**
     * 查询企业列表（不分页）
     */
    public List<Company> companyList(String name, CompanyType type, CompanyStatus status) {
        // 1. 获取当前登陆用户的企业ID
        Long currentCompanyId = SecurityUtils.getCompanyId();
        
        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<>();
        
        // 2. 【核心权限控制】处理数据隔离
        if (currentCompanyId != null) {
            // 获取当前企业的信息，主要是为了拿到它的精准 path
            Company currentCompany = this.baseMapper.selectById(currentCompanyId);
            if (currentCompany == null) {
                return new ArrayList<>(); // 容错处理：企业被物理删除了则返回空
            }
            
            // 🚀 神级过滤：查自己 + 所有下级！
            // MyBatis-Plus 的 likeRight 会生成 SQL: path LIKE '0,1,5,%'
            // 注意：千万不要用 like()，like() 会生成 '%0,1,5,%'，导致索引失效全表扫描！
            wrapper.likeRight(Company::getPath, currentCompany.getPath());
        } else {
            // 如果 currentCompanyId 为 null，说明是框架的超级管理员 admin
            // 不加 path 过滤，直接放行，拥有查看全国所有企业的上帝视角
        }
        
        // 3. 动态拼接前端传来的普通查询条件
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(Company::getName, name); // 名称允许全模糊搜索
        }
        
        if (status != null) {
            wrapper.eq(Company::getStatus, status);
        }
        
        if (type != null) {
            // 💡 适配我们之前设计的“多重身份”布尔值字段
            // 因为你的入参是 CompanyType 枚举，我们需要把它翻译成具体的字段查询
            switch (type) {
                case MANUFACTURER: // 制造商
                    wrapper.eq(Company::getTypeManufacturer, 1);
                    break;
                case DISTRIBUTOR:       // 经销商
                    wrapper.eq(Company::getTypeDealer, 1);
                    break;
                case RETAILER:       // 充气站
                    wrapper.eq(Company::getTypeFiller, 1);
                    break;
                case INSPECTION:   // 年检机构
                    wrapper.eq(Company::getTypeInspection, 1);
                    break;
            }
        }
        
        // 4. 按创建时间倒序排，新开的网点在前面
        wrapper.orderByDesc(Company::getCreateTime);
        
        // 5. 执行查询并返回
        return this.baseMapper.selectList(wrapper);
    }
}
