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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppUser;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;
import me.zhengjie.modules.maint.domain.cylinder.entity.Cylinder;
import me.zhengjie.modules.maint.domain.cylinder.entity.CylinderFlow;
import me.zhengjie.modules.maint.domain.cylinder.entity.OperationLog;
import me.zhengjie.modules.maint.domain.cylinder.mapper.AppUserMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CompanyMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CylinderFlowMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CylinderMapper;
import me.zhengjie.modules.maint.domain.cylinder.mapper.OperationLogMapper;
import me.zhengjie.modules.maint.domain.dto.OperationLogPageDto;
import me.zhengjie.modules.maint.domain.enums.TargetType;
import me.zhengjie.modules.maint.rest.command.OperationLogQueryReq;
import me.zhengjie.modules.maint.util.SecurityContext;
import me.zhengjie.utils.PageResult;
import me.zhengjie.utils.PageUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 操作日志
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService extends ServiceImpl<OperationLogMapper, OperationLog> {
    
    private final AppUserMapper userMapper;
    private final CylinderMapper cylinderMapper;
    private final CompanyMapper companyMapper;
    private final CylinderFlowMapper cylinderFlowMapper;
    

    /**
     * 分页查询当前用户操作日志（包含关联信息）
     */
    public PageResult<OperationLogPageDto> queryOrgOperationLogPage(OperationLogQueryReq req) {
        Boolean isAdmin = SecurityContext.getCurrentUser().getUser().getIsAdmin();
        Long currentCompanyId = SecurityContext.getCompanyId();
        
        // 执行分页查询
        Page<OperationLog> page = new Page<>(req.getPage(), req.getSize());
        this.baseMapper.selectPageByCompany(page, isAdmin ? null : currentCompanyId, req.getOperation(), req.getTargetType(), req.getTargetId(), req.getQrcode(), req.getIp(), req.getStartTime(), req.getEndTime());
        
        List<OperationLog> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return new PageResult<>(new ArrayList<>(), page.getTotal());
        }
        
        // 收集用户ID（虽然都是当前用户，但保持一致性）
        Set<Long> userIds = records.stream()
                                   .map(OperationLog::getUserId)
                                   .collect(Collectors.toSet());
        Map<Long, String> userNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIds)) {
            List<AppUser> users = userMapper.selectBatchIds(userIds);
            userNameMap = users.stream()
                               .collect(Collectors.toMap(AppUser::getId, AppUser::getUsername));
        }
        
        // 收集关联ID，用于批量查询气瓶和企业
        Set<Long> cylinderIds = records.stream()
                                       .filter(log -> log.getTargetType() == TargetType.CYLINDER)
                                       .map(OperationLog::getTargetId)
                                       .collect(Collectors.toSet());
        Set<Long> companyIds = records.stream()
                                      .filter(log -> log.getTargetType() == TargetType.COMPANY)
                                      .map(OperationLog::getTargetId)
                                      .collect(Collectors.toSet());
        Set<Long> flowIds = records.stream()
                                   .filter(log -> log.getFlowId() != null)
                                   .map(OperationLog::getFlowId)
                                   .collect(Collectors.toSet());
        
        // 批量查询气瓶
        Map<Long, Cylinder> cylinderMap = new HashMap<>();
        if (CollUtil.isNotEmpty(cylinderIds)) {
            List<Cylinder> cylinders = cylinderMapper.selectBatchIds(cylinderIds);
            cylinderMap = cylinders.stream()
                                   .collect(Collectors.toMap(Cylinder::getId, c -> c));
        }
        
        // 批量查询企业
        Map<Long, Company> companyMap = new HashMap<>();
        if (CollUtil.isNotEmpty(companyIds)) {
            List<Company> companies = companyMapper.selectBatchIds(companyIds);
            companyMap = companies.stream()
                                  .collect(Collectors.toMap(Company::getId, c -> c));
        }
        
        // 批量查询流转记录
        Map<Long, CylinderFlow> flowMap = new HashMap<>();
        if (CollUtil.isNotEmpty(flowIds)) {
            List<CylinderFlow> flows = cylinderFlowMapper.selectBatchIds(flowIds);
            flowMap = flows.stream()
                           .collect(Collectors.toMap(CylinderFlow::getId, f -> f));
        }
        
        // 收集流转记录中的企业ID，用于查询企业名称
        Set<Long> flowCompanyIds = new HashSet<>();
        flowMap.values().forEach(flow -> {
            if (flow.getFromCompanyId() != null) flowCompanyIds.add(flow.getFromCompanyId());
            if (flow.getToCompanyId() != null) flowCompanyIds.add(flow.getToCompanyId());
        });
        if (CollUtil.isNotEmpty(flowCompanyIds)) {
            List<Company> flowCompanies = companyMapper.selectBatchIds(flowCompanyIds);
            Map<Long, String> flowCompanyNameMap = flowCompanies.stream()
                                                                .collect(Collectors.toMap(Company::getId, Company::getName));
            // 将名称设置到flow中
            flowMap.values().forEach(flow -> {
                flow.setFromCompanyName(flowCompanyNameMap.get(flow.getFromCompanyId()));
                flow.setToCompanyName(flowCompanyNameMap.get(flow.getToCompanyId()));
            });
        }
        
        // 转换为DTO
        Map<Long, String> finalUserNameMap = userNameMap;
        Map<Long, Cylinder> finalCylinderMap = cylinderMap;
        Map<Long, Company> finalCompanyMap = companyMap;
        Map<Long, CylinderFlow> finalFlowMap = flowMap;
        
        List<OperationLogPageDto> dtoList = records.stream().map(log -> {
            OperationLogPageDto dto = new OperationLogPageDto();
            BeanUtils.copyProperties(log, dto);
            dto.setUsername(finalUserNameMap.getOrDefault(log.getUserId(), "未知用户"));
            
            // 设置枚举中文名称
            if (log.getOperation() != null) {
                dto.setOperationName(log.getOperation().getName());
            }
            if (log.getTargetType() != null) {
                dto.setTargetTypeName(log.getTargetType().getName());
            }
            
            // 设置关联信息
            if (log.getTargetType() == TargetType.CYLINDER && log.getTargetId() != null) {
                Cylinder cylinder = finalCylinderMap.get(log.getTargetId());
                if (cylinder != null) {
                    OperationLogPageDto.CylinderInfo cylinderInfo = new OperationLogPageDto.CylinderInfo();
                    cylinderInfo.setId(cylinder.getId());
                    cylinderInfo.setCode(cylinder.getCode());
                    cylinderInfo.setSpec(cylinder.getSpec());
                    cylinderInfo.setVolume(cylinder.getVolume());
                    
                    // 如果有流转记录，设置来源和目标企业名称
                    if (log.getFlowId() != null) {
                        CylinderFlow flow = finalFlowMap.get(log.getFlowId());
                        if (flow != null) {
                            cylinderInfo.setFromCompanyName(flow.getFromCompanyName());
                            cylinderInfo.setToCompanyName(flow.getToCompanyName());
                        }
                    }
                    
                    dto.setCylinderInfo(cylinderInfo);
                }
            } else if (log.getTargetType() == TargetType.COMPANY && log.getTargetId() != null) {
                Company company = finalCompanyMap.get(log.getTargetId());
                if (company != null) {
                    OperationLogPageDto.CompanyInfo companyInfo = new OperationLogPageDto.CompanyInfo();
                    companyInfo.setId(company.getId());
                    companyInfo.setName(company.getName());
                    companyInfo.setCode(company.getCode());
                    dto.setCompanyInfo(companyInfo);
                }
            }
            
            return dto;
        }).collect(Collectors.toList());
        
        return PageUtil.toPage(dtoList, page.getTotal());
    }
    
    

    /**
     * 分页查询当前用户操作日志（包含关联信息）
     */
    public PageResult<OperationLogPageDto> queryCurrentUserOperationLogPage(OperationLogQueryReq req) {
        // 获取当前用户ID
        Long currentUserId = SecurityContext.getUserId();
        
        // 构建查询条件，强制过滤当前用户
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getUserId, currentUserId); // 关键：只查询当前用户的日志
        
        // 其他查询条件（与原有方法类似，但可以根据需求调整）
        if (req.getOperation() != null) {
            wrapper.eq(OperationLog::getOperation, req.getOperation());
        }
        if (req.getTargetType() != null) {
            wrapper.eq(OperationLog::getTargetType, req.getTargetType());
        }
        if (req.getTargetId() != null) {
            wrapper.eq(OperationLog::getTargetId, req.getTargetId());
        }
        if (req.getIp() != null && !req.getIp().trim().isEmpty()) {
            wrapper.like(OperationLog::getIp, req.getIp().trim());
        }
        if (req.getStartTime() != null) {
            wrapper.ge(OperationLog::getCreateTime, req.getStartTime());
        }
        if (req.getEndTime() != null) {
            wrapper.le(OperationLog::getCreateTime, req.getEndTime());
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);
        
        // 执行分页查询
        Page<OperationLog> page = new Page<>(req.getPage(), req.getSize());
        this.baseMapper.selectPage(page, wrapper);
        
        List<OperationLog> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return new PageResult<>(new ArrayList<>(), page.getTotal());
        }
        
        // 收集用户ID（虽然都是当前用户，但保持一致性）
        Set<Long> userIds = records.stream()
                .map(OperationLog::getUserId)
                .collect(Collectors.toSet());
        Map<Long, String> userNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIds)) {
            List<AppUser> users = userMapper.selectBatchIds(userIds);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(AppUser::getId, AppUser::getUsername));
        }
        
        // 收集关联ID，用于批量查询气瓶和企业
        Set<Long> cylinderIds = records.stream()
                .filter(log -> log.getTargetType() == TargetType.CYLINDER)
                .map(OperationLog::getTargetId)
                .collect(Collectors.toSet());
        Set<Long> companyIds = records.stream()
                .filter(log -> log.getTargetType() == TargetType.COMPANY)
                .map(OperationLog::getTargetId)
                .collect(Collectors.toSet());
        Set<Long> flowIds = records.stream()
                .filter(log -> log.getFlowId() != null)
                .map(OperationLog::getFlowId)
                .collect(Collectors.toSet());
        
        // 批量查询气瓶
        Map<Long, Cylinder> cylinderMap = new HashMap<>();
        if (CollUtil.isNotEmpty(cylinderIds)) {
            List<Cylinder> cylinders = cylinderMapper.selectBatchIds(cylinderIds);
            cylinderMap = cylinders.stream()
                    .collect(Collectors.toMap(Cylinder::getId, c -> c));
        }
        
        // 批量查询企业
        Map<Long, Company> companyMap = new HashMap<>();
        if (CollUtil.isNotEmpty(companyIds)) {
            List<Company> companies = companyMapper.selectBatchIds(companyIds);
            companyMap = companies.stream()
                    .collect(Collectors.toMap(Company::getId, c -> c));
        }
        
        // 批量查询流转记录
        Map<Long, CylinderFlow> flowMap = new HashMap<>();
        if (CollUtil.isNotEmpty(flowIds)) {
            List<CylinderFlow> flows = cylinderFlowMapper.selectBatchIds(flowIds);
            flowMap = flows.stream()
                    .collect(Collectors.toMap(CylinderFlow::getId, f -> f));
        }
        
        // 收集流转记录中的企业ID，用于查询企业名称
        Set<Long> flowCompanyIds = new HashSet<>();
        flowMap.values().forEach(flow -> {
            if (flow.getFromCompanyId() != null) flowCompanyIds.add(flow.getFromCompanyId());
            if (flow.getToCompanyId() != null) flowCompanyIds.add(flow.getToCompanyId());
        });
        if (CollUtil.isNotEmpty(flowCompanyIds)) {
            List<Company> flowCompanies = companyMapper.selectBatchIds(flowCompanyIds);
            Map<Long, String> flowCompanyNameMap = flowCompanies.stream()
                    .collect(Collectors.toMap(Company::getId, Company::getName));
            // 将名称设置到flow中
            flowMap.values().forEach(flow -> {
                flow.setFromCompanyName(flowCompanyNameMap.get(flow.getFromCompanyId()));
                flow.setToCompanyName(flowCompanyNameMap.get(flow.getToCompanyId()));
            });
        }
        
        // 转换为DTO
        Map<Long, String> finalUserNameMap = userNameMap;
        Map<Long, Cylinder> finalCylinderMap = cylinderMap;
        Map<Long, Company> finalCompanyMap = companyMap;
        Map<Long, CylinderFlow> finalFlowMap = flowMap;
        
        List<OperationLogPageDto> dtoList = records.stream().map(log -> {
            OperationLogPageDto dto = new OperationLogPageDto();
            BeanUtils.copyProperties(log, dto);
            dto.setUsername(finalUserNameMap.getOrDefault(log.getUserId(), "未知用户"));
            
            // 设置枚举中文名称
            if (log.getOperation() != null) {
                dto.setOperationName(log.getOperation().getName());
            }
            if (log.getTargetType() != null) {
                dto.setTargetTypeName(log.getTargetType().getName());
            }
            
            // 设置关联信息
            if (log.getTargetType() == TargetType.CYLINDER && log.getTargetId() != null) {
                Cylinder cylinder = finalCylinderMap.get(log.getTargetId());
                if (cylinder != null) {
                    OperationLogPageDto.CylinderInfo cylinderInfo = new OperationLogPageDto.CylinderInfo();
                    cylinderInfo.setId(cylinder.getId());
                    cylinderInfo.setCode(cylinder.getCode());
                    cylinderInfo.setSpec(cylinder.getSpec());
                    cylinderInfo.setVolume(cylinder.getVolume());
                    
                    // 如果有流转记录，设置来源和目标企业名称
                    if (log.getFlowId() != null) {
                        CylinderFlow flow = finalFlowMap.get(log.getFlowId());
                        if (flow != null) {
                            cylinderInfo.setFromCompanyName(flow.getFromCompanyName());
                            cylinderInfo.setToCompanyName(flow.getToCompanyName());
                        }
                    }
                    
                    dto.setCylinderInfo(cylinderInfo);
                }
            } else if (log.getTargetType() == TargetType.COMPANY && log.getTargetId() != null) {
                Company company = finalCompanyMap.get(log.getTargetId());
                if (company != null) {
                    OperationLogPageDto.CompanyInfo companyInfo = new OperationLogPageDto.CompanyInfo();
                    companyInfo.setId(company.getId());
                    companyInfo.setName(company.getName());
                    companyInfo.setCode(company.getCode());
                    dto.setCompanyInfo(companyInfo);
                }
            }
            
            return dto;
        }).collect(Collectors.toList());
        
        return PageUtil.toPage(dtoList, page.getTotal());
    }
}
