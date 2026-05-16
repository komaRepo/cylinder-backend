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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.exception.BusinessException;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;
import me.zhengjie.modules.maint.domain.cylinder.entity.SysUserCompany;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CompanyMapper;
import me.zhengjie.modules.maint.domain.dto.CompanyVo;
import me.zhengjie.modules.maint.domain.dto.CompanyWithAccountsDto;
import me.zhengjie.modules.maint.domain.dto.UserInfoDTO;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.mapper.UserMapper;
import me.zhengjie.modules.maint.domain.enums.CompanyStatus;
import me.zhengjie.modules.maint.domain.enums.CompanyType;
import me.zhengjie.modules.maint.util.SecurityContext;
import me.zhengjie.sys.ResultCodeEnum;
import me.zhengjie.utils.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    
    private final AppRoleService appRoleService;
    private final SysUserCompanyService sysUserCompanyService;
    private final UserMapper userMapper;
    
    /**
     * 管理后台企业注册
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(CompanyType type, String name, String creditCode, String code, String legalName,
                         String legalCode, String contactName, String contactPhone, String countryCode, String province,
                         String city, String district, String address, Long parentId, String businessLicense,
                         String dangerBusinessLicense, String cylinderFillLicense, String specialEquipmentLicense,
                         BigDecimal longitude, BigDecimal latitude) {
        
        // 1. 参数校验
        _checkParam(type, name, creditCode, legalName, legalCode, province, city, district, businessLicense, dangerBusinessLicense, cylinderFillLicense, specialEquipmentLicense);
        
        // 2. 注册企业基础信息
        // 唯一性校验：企业名称不得重复
        if (StrUtil.isBlank(name)) {
            throw new IllegalArgumentException("企业名称不能为空");
        }
        QueryWrapper<Company> nameCheck = new QueryWrapper<>();
        nameCheck.lambda().eq(Company::getName, name.trim());
        Long exists = this.baseMapper.selectCount(nameCheck);
        if (exists != null && exists > 0) {
            throw new BusinessException("企业名称已存在");
        }
        
        Company company = new Company();
        company.setName(name);
        company.setCode(code);
        company.setCreditCode(creditCode);
        company.setLegalName(legalName);
        company.setLegalCode(legalCode);
        company.setContact(contactName);
        company.setPhone(contactPhone);
        company.setCountryCode(countryCode);
        company.setProvince(province);
        company.setCity(city);
        company.setDistrict(district);
        company.setAddress(address);
        company.setLatitude(latitude);
        company.setLongitude(longitude);
        // 注意：如果前端传了 null，这里给个默认值 0，代表它是顶级节点
        company.setParentId(parentId != null ? parentId : 0L);
        company.setStatus(CompanyStatus.INACTIVE); // 默认待审核状态
        
        // ==========================================
        // 3. 【新增逻辑】根据传入的枚举，自动激活企业的资质身份
        // ==========================================
        if (type != null) {
            company.setTypeManufacturer(CompanyType.MANUFACTURER.equals(type) ? 1 : 0);
            company.setTypeDealer(CompanyType.DISTRIBUTOR.equals(type) ? 1 : 0);
            company.setTypeFiller(CompanyType.RETAILER.equals(type) ? 1 : 0);
            company.setTypeInspection(CompanyType.INSPECTION.equals(type) ? 1 : 0);
        }
        
        // 4. 第一次落库，生成企业主键 ID
        this.baseMapper.insert(company);
        
        // ==========================================
        // 5. 【核心修复】计算层级路径 Path
        // ==========================================
        String path;
        // 明确判断：只有当 parentId 存在且大于 0 时，才说明它有上级
        if (parentId != null && parentId > 0L) {
            Company parent = this.baseMapper.selectById(parentId);
            // 防御性编程：防止前端传了一个不存在的上级 ID
            if (parent == null) {
                throw new BusinessException(ResultCodeEnum.PARENT_COMPANY_NOT_EXIST);
            }
            // 拼接路径：上级路径 + 自己的ID + 逗号
            path = parent.getPath().concat(company.getId().toString()).concat(",");
        } else {
            // 顶级节点（parentId 为 0）：路径就是它自己
            // 比如它的 ID 是 5，那它的 path 就是 "5,"
            // 也有一些大厂规范喜欢加上 "0," 作为绝对根目录，即 "0,5,"，这里按你之前的逻辑保持 "5,"
            path = company.getId().toString().concat(",");
        }
        
        // 6. 更新路径信息
        company.setPath(path);
        this.baseMapper.updateById(company);
        
        // 7. 初始化默认角色
        appRoleService.initDefaultRolesForCompany(company.getId(), type);
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
                //分销商必须提供营业执license和危险化学品经营许可证
                if (ObjectUtil.hasEmpty(businessLicense, dangerBusinessLicense)) {
                    throw new IllegalArgumentException("分销商必须提供营业执照和危险化学品经营许可证");
                }
        } else if (ObjectUtil.equals(type, CompanyType.RETAILER)) {
                //加气站必须提供营业执license、危险化学品经营许可证、压力容器充装许可证和特种设备许可证
                if (ObjectUtil.hasEmpty(businessLicense, dangerBusinessLicense, cylinderFillLicense, specialEquipmentLicense)) {
                    throw new IllegalArgumentException("加气站必须提供营业执照、危险化学品经营许可证、压力容器充装许可证和特种设备许可证");
                }
        }
        
    }
    
    
    /**
     * 查询企业列表（不分页）
     */
    public List<Company> companyList(String name, CompanyType type, CompanyStatus status) {
        Boolean isAdmin = SecurityContext.getCurrentUser().getUser().getIsAdmin();
        // 1. 获取当前登陆用户的企业ID
        Long currentCompanyId = SecurityContext.getCompanyId();
        
        QueryWrapper<Company> wrapper = new QueryWrapper<>();
        
        // 2. 【核心权限控制】处理数据隔离
        if (!isAdmin) {
            if (currentCompanyId != null) {
                // 获取当前企业的信息，主要是为了拿到它的精准 path
                Company currentCompany = this.baseMapper.selectById(currentCompanyId);
                if (currentCompany == null) {
                    return new ArrayList<>(); // 容错处理：企业被物理删除了则返回空
                }
                
                // 🚀 神级过滤：查自己 + 所有下级！
                // MyBatis-Plus 的 likeRight 会生成 SQL: path LIKE '0,1,5,%'
                // 注意：千万不要用 like()，like() 会生成 '%0,1,5,%'，导致索引失效全表扫描！
                wrapper.lambda().likeRight(Company::getPath, currentCompany.getPath());
            } else {
                throw new BusinessException(ResultCodeEnum.COMPANY_NOT_BIND);
            }
        }
        
        // 3. 动态拼接前端传来的普通查询条件
        if (StrUtil.isNotBlank(name)) {
            wrapper.lambda().like(Company::getName, name); // 名称允许全模糊搜索
        }
        
        if (status != null) {
            wrapper.lambda().eq(Company::getStatus, status);
        }
        
        if (type != null) {
            // 💡 适配我们之前设计的"多重身份"布尔值字段
            // 因为你的入参是 CompanyType 枚举，我们需要把它翻译成具体的字段查询
            switch (type) {
                case MANUFACTURER: // 制造商
                    wrapper.lambda().eq(Company::getTypeManufacturer, 1);
                    break;
                case DISTRIBUTOR:       // 经销商
                    wrapper.lambda().eq(Company::getTypeDealer, 1);
                    break;
                case RETAILER:       // 充气站
                    wrapper.lambda().eq(Company::getTypeFiller, 1);
                    break;
                case INSPECTION:   // 年检机构
                    wrapper.lambda().eq(Company::getTypeInspection, 1);
                    break;
            }
        }
        
        // 4. 按创建时间倒序排，新开的网点在前面
        wrapper.lambda().orderByDesc(Company::getCreateTime);
        
        // 5. 只返回绑定了后台账号的企业
        wrapper.apply("exists (select 1 from sys_user_company where company_id = company.id)");
        
        // 6. 执行查询并返回
        return this.baseMapper.selectList(wrapper);
    }
    
    /**
     * 查询企业列表（分页），包含绑定的账号信息
     */
    public PageResult<CompanyWithAccountsDto> companyListWithAccounts(String name, CompanyType type, CompanyStatus status, Integer page, Integer size) {
        Boolean isAdmin = SecurityContext.getCurrentUser().getUser().getIsAdmin();
        // 1. 获取当前登陆用户的企业ID
        Long currentCompanyId = SecurityContext.getCompanyId();
        
        Page<Company> pageObj = new Page<>(page, size);
        
        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<>();
        
        if (!isAdmin) {
            // 2. 【核心权限控制】处理数据隔离
            if (currentCompanyId != null) {
                // 获取当前企业的信息，主要是为了拿到它的精准 path
                Company currentCompany = this.baseMapper.selectById(currentCompanyId);
                if (currentCompany == null) {
                    return new PageResult<>(new ArrayList<>(), 0L);
                }
                
                // 🚀 神级过滤：查自己 + 所有下级！
                // MyBatis-Plus 的 likeRight 会生成 SQL: path LIKE '0,1,5,%'
                // 注意：千万不要用 like()，like() 会生成 '%0,1,5,%'，导致索引失效全表扫描！
                wrapper.likeRight(Company::getPath, currentCompany.getPath());
            } else {
                throw new BusinessException(ResultCodeEnum.COMPANY_NOT_BIND);
            }
        }
        
        // 3. 动态拼接前端传来的普通查询条件
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(Company::getName, name); // 名称允许全模糊搜索
        }
        
        if (status != null) {
            wrapper.eq(Company::getStatus, status);
        }
        
        if (type != null) {
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
        
        // 6. 执行分页查询
        Page<Company> companyPage = this.baseMapper.selectPage(pageObj, wrapper);
        List<Company> companies = companyPage.getRecords();
        
        // 7. 获取所有企业的绑定账号信息
        List<CompanyWithAccountsDto> result = new ArrayList<>();
        if (CollUtil.isNotEmpty(companies)) {
            // 获取所有公司ID
            List<Long> companyIds = companies.stream().map(Company::getId).collect(java.util.stream.Collectors.toList());
            
            // 查询绑定关系
            LambdaQueryWrapper<SysUserCompany> bindWrapper = new LambdaQueryWrapper<>();
            bindWrapper.in(SysUserCompany::getCompanyId, companyIds);
            List<SysUserCompany> bindings = sysUserCompanyService.list(bindWrapper);
            
            // 按公司分组用户ID
            java.util.Map<Long, List<Long>> companyUserMap = bindings.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    SysUserCompany::getCompanyId,
                    java.util.stream.Collectors.mapping(SysUserCompany::getUserId, java.util.stream.Collectors.toList())
                ));
            
            // 获取所有用户ID
            List<Long> allUserIds = bindings.stream().map(SysUserCompany::getUserId).distinct().collect(java.util.stream.Collectors.toList());
            
            // 批量查询用户
            List<User> users = CollUtil.isNotEmpty(allUserIds) ? userMapper.selectBatchIds(allUserIds) : new ArrayList<>();
            java.util.Map<Long, User> userMap = users.stream().collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
            
            // 组装结果
            for (Company company : companies) {
                CompanyWithAccountsDto dto = new CompanyWithAccountsDto();
                dto.setCompany(company);
                
                List<Long> userIds = companyUserMap.get(company.getId());
                if (CollUtil.isNotEmpty(userIds)) {
                    List<UserInfoDTO> boundAccounts = userIds.stream()
                        .map(userId -> {
                            User user = userMap.get(userId);
                            return user != null ? convertToUserInfoDTO(user) : null;
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList());
                    dto.setBoundAccounts(boundAccounts);
                } else {
                    dto.setBoundAccounts(new ArrayList<>());
                }
                
                result.add(dto);
            }
        }
        
        return new PageResult<>(result, companyPage.getTotal());
    }
    
    /**
     * 将User转换为UserInfoDTO
     */
    private UserInfoDTO convertToUserInfoDTO(User user) {
        UserInfoDTO dto = new UserInfoDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setPhone(user.getPhone());
        // User类中没有userType和status字段，暂时设为默认值
        dto.setUserType(2); // 默认普通员工
        dto.setStatus(user.getEnabled() ? 1 : 2); // 1正常 2禁用
        // 其他字段如果需要可以添加
        return dto;
    }
    
    /**
     * 查询所有企业(app端注册时选择企业用)
     * @return
     */
    public List<CompanyVo> listAll() {
        QueryWrapper<Company> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .lambda()
                .eq(Company::getStatus, CompanyStatus.INACTIVE);
        // 只返回已绑定管理端账号的企业
        queryWrapper.apply("exists (select 1 from sys_user_company where company_id = company.id)");
        List<Company> companies = this.baseMapper.selectList(queryWrapper);
        if (CollUtil.isEmpty(companies)) {
            return new ArrayList<>();
        }

        return CompanyVo.Converter.INSTANCE.fromEntityList(companies);
    }
    
}
