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

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.maint.domain.cylinder.entity.ScanRecord;
import me.zhengjie.modules.maint.domain.cylinder.mapper.ScanRecordMapper;
import org.springframework.stereotype.Service;

/**
 *
 * @author koma at cylinder-backend
 * @since 2026/3/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanRecordService extends ServiceImpl<ScanRecordMapper, ScanRecord> {

    
}
