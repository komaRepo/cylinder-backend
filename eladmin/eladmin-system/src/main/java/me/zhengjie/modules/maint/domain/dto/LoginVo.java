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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.zhengjie.modules.maint.domain.enums.UserType;

import java.util.Set;

/**
 *
 * @author koma at cylinder-backend
 * @since 2026/3/23
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginVo {
    
    private String token;
    
    // 2. 权限集合 (前端拿到后直接存本地，用于控制按钮显示/隐藏)
    private Set<String> permissions;
    
    // 3. 用户基本信息
    private UserInfo userInfo;
    
    // 4. 所属企业信息
    private CompanyInfo companyInfo;
    
    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String username;
        private String phone;
        private UserType userType; // 或者你的 UserType 枚举
    }
    
    @Data
    @Builder
    public static class CompanyInfo {
        private Long id;
        private String name;
        private String code;
        // 把前端最关心的企业资质传过去，方便前端做大模块的路由拦截
        private Integer typeManufacturer;
        private Integer typeDealer;
        private Integer typeFiller;
        private Integer typeInspection;
    }
    
}
