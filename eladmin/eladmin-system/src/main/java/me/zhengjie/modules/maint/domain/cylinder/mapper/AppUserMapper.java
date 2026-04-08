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
package me.zhengjie.modules.maint.domain.cylinder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.zhengjie.modules.maint.domain.cylinder.entity.AppUser;
import me.zhengjie.modules.maint.domain.dto.AppUserDetail;
import me.zhengjie.modules.maint.domain.enums.UserStatus;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

/**
 * TODO
 *
 * @author koma at rfid-backend
 * @since 2026/3/17
 */
@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
    
    
    Page<AppUserDetail> fetchUserList(
            @RequestParam("companyId") Long companyId,
            @RequestParam("roleId") Long roleId,
            @RequestParam("username") String username,
            @RequestParam("phone") String phone,
            @RequestParam("status") UserStatus status,
            @RequestParam("createTimeStart") Date createTimeStart,
            @RequestParam("createTimeEnd") Date createTimeEnd,
            Page<Object> page);
}
