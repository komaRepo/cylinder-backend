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
package me.zhengjie.modules.maint.domain.cylinder.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.zhengjie.modules.maint.domain.cylinder.entity.ScanRecord;
import me.zhengjie.modules.maint.domain.dto.ScanRecordPageDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * TODO
 *
 * @author koma at cylinder-backend
 * @since 2026/3/18
 */
@Mapper
public interface ScanRecordMapper extends BaseMapper<ScanRecord> {

    IPage<ScanRecordPageDto> pageQuery(IPage<ScanRecordPageDto> page,
                                       @Param("companyId") Long companyId,
                                       @Param("scanType") Integer scanType,
                                       @Param("cylinderCode") String cylinderCode,
                                       @Param("userId") Long userId,
                                       @Param("startTime") Date startTime,
                                       @Param("endTime") Date endTime);
}
