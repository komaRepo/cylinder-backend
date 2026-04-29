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
import me.zhengjie.modules.maint.domain.cylinder.entity.OperationLog;
import me.zhengjie.modules.maint.domain.cylinder.mapper.OperationLogMapper;
import me.zhengjie.modules.maint.domain.dto.OperationLogPageDto;
import me.zhengjie.modules.maint.rest.command.OperationLogQueryReq;
import me.zhengjie.modules.maint.util.SecurityContext;
import me.zhengjie.modules.system.domain.User;
import me.zhengjie.modules.system.mapper.UserMapper;
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
    
    private final UserMapper userMapper;
    
    /**
     * 分页查询操作日志
     */
    public PageResult<OperationLogPageDto> queryOperationLogPage(OperationLogQueryReq req) {
        // 构建查询条件
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        // 操作类型
        if (req.getOperation() != null) {
            wrapper.eq(OperationLog::getOperation, req.getOperation());
        }
        
        // 操作对象类型
        if (req.getTargetType() != null) {
            wrapper.eq(OperationLog::getTargetType, req.getTargetType());
        }
        
        // 对象ID
        if (req.getTargetId() != null) {
            wrapper.eq(OperationLog::getTargetId, req.getTargetId());
        }
        
        // 操作用户ID
        if (req.getUserId() != null) {
            wrapper.eq(OperationLog::getUserId, req.getUserId());
        }
        
        // IP地址
        if (req.getIp() != null && !req.getIp().trim().isEmpty()) {
            wrapper.like(OperationLog::getIp, req.getIp().trim());
        }
        
        // 时间范围
        if (req.getStartTime() != null) {
            wrapper.ge(OperationLog::getCreateTime, req.getStartTime());
        }
        if (req.getEndTime() != null) {
            wrapper.le(OperationLog::getCreateTime, req.getEndTime());
        }
        
        // 默认按时间倒序
        wrapper.orderByDesc(OperationLog::getCreateTime);
        
        // 执行分页查询
        Page<OperationLog> page = new Page<>(req.getPage(), req.getSize());
        this.baseMapper.selectPage(page, wrapper);
        
        List<OperationLog> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return new PageResult<>(new ArrayList<>(), page.getTotal());
        }
        
        // 收集用户ID，用于批量查询用户名
        Set<Long> userIds = records.stream()
                .map(OperationLog::getUserId)
                .collect(Collectors.toSet());
        
        // 批量查询用户
        Map<Long, String> userNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIds)) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getUsername));
        }
        
        // 转换为DTO
        Map<Long, String> finalUserNameMap = userNameMap;
        List<OperationLogPageDto> dtoList = records.stream().map(log -> {
            OperationLogPageDto dto = new OperationLogPageDto();
            BeanUtils.copyProperties(log, dto);
            dto.setUsername(finalUserNameMap.getOrDefault(log.getUserId(), "未知用户"));
            return dto;
        }).collect(Collectors.toList());
        
        return PageUtil.toPage(dtoList, page.getTotal());
    }
}
