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
package me.zhengjie.modules.maint.domain.cylinder;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.entity.ScanRecord;
import me.zhengjie.modules.maint.domain.cylinder.mapper.ScanRecordMapper;
import me.zhengjie.modules.maint.domain.dto.ScanRecordPageDto;
import me.zhengjie.modules.maint.rest.command.ScanRecordQueryReq;
import me.zhengjie.modules.maint.util.SecurityContext;
import me.zhengjie.utils.PageResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import me.zhengjie.modules.maint.domain.enums.ScanType;
import me.zhengjie.modules.maint.domain.cylinder.mapper.CompanyMapper;
import me.zhengjie.modules.maint.domain.cylinder.entity.Company;

/**
 *
 * @author koma at cylinder-backend
 * @since 2026/3/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanRecordService extends ServiceImpl<ScanRecordMapper, ScanRecord> {

    private final CompanyMapper companyMapper;

    public PageResult<ScanRecordPageDto> pageQuery(ScanRecordQueryReq req) {
        Boolean isAdmin = SecurityContext.getCurrentUser().getUser().getIsAdmin();
        Long currentCompanyId = SecurityContext.getCompanyId();
        Page<ScanRecordPageDto> page = new Page<>(req.getPage(), req.getSize());
        Page<ScanRecordPageDto> resultPage = (Page<ScanRecordPageDto>) this.baseMapper.pageQuery(
                page,
                isAdmin ? null : currentCompanyId, // admin用户传null，不加company_id过滤
                req.getScanType(),
                req.getQrcode(),
                req.getUserId(),
                req.getStartTime(),
                req.getEndTime()
        );

        // 为扫码类型设置中文名称
        List<ScanRecordPageDto> records = resultPage.getRecords();
        if (CollUtil.isNotEmpty(records)) {
            // 收集需要查询的企业ID
            Set<Long> companyIds = records.stream()
                    .map(ScanRecordPageDto::getCompanyId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            // 批量查询企业信息
            Map<Long, String> companyNameMap = new HashMap<>();
            if (CollUtil.isNotEmpty(companyIds)) {
                List<Company> companies = companyMapper.selectBatchIds(companyIds);
                companyNameMap = companies.stream()
                        .collect(Collectors.toMap(Company::getId, Company::getName));
            }
            
            // 设置中文名称
            Map<Long, String> finalCompanyNameMap = companyNameMap;
            records.forEach(record -> {
                if (record.getScanType() != null) {
                    record.setScanTypeName(getScanTypeName(record.getScanType()));
                }
                if (record.getCompanyId() != null) {
                    record.setCompanyName(finalCompanyNameMap.getOrDefault(record.getCompanyId(), "未知企业"));
                }
            });
        }

        return new PageResult<>(records, resultPage.getTotal());
    }

    /**
     * 获取扫码类型中文名称
     */
    private String getScanTypeName(Integer scanType) {
        for (ScanType type : ScanType.values()) {
            if (type.getCode() == scanType) {
                return type.getName();
            }
        }
        return "未知";
    }
}
