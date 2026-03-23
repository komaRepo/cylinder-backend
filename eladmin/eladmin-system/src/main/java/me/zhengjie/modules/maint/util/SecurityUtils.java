package me.zhengjie.modules.maint.util;

import me.zhengjie.exception.BusinessException;
import me.zhengjie.modules.security.service.dto.JwtUserDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 极简版获取当前登录用户上下文工具类
 */
public class SecurityUtils {

    /**
     * 获取当前登录的完整用户信息
     */
    public static JwtUserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(401, "登录状态已过期");
        }
        return (JwtUserDto) authentication.getPrincipal();
    }

    /**
     * 快捷获取：当前登录用户ID
     */
    public static Long getUserId() {
        return getCurrentUser().getUserId();
    }

    /**
     * 快捷获取：当前登录人所属企业ID
     */
    public static Long getCompanyId() {
        return getCurrentUser().getCompanyId();
    }

    /**
     * 快捷获取：当前登录人的企业层级Path（用于极速树形查询）
     */
    public static String getCompanyPath() {
        return getCurrentUser().getCompanyPath();
    }
    
    /**
     * 快捷获取：当前登录用户名
     */
    public static String getCurrentUserName() {
        return getCurrentUser().getUsername();
    }
}