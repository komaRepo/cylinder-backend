package me.zhengjie.modules.maint.util;

import me.zhengjie.exception.BusinessException;
import me.zhengjie.modules.security.service.dto.JwtUserDto;
import me.zhengjie.sys.ResultCodeEnum;
import me.zhengjie.utils.SpringBeanHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

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
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        
        Object principal = authentication.getPrincipal();
        String smartSubject; // 此时拿到的不再是单纯的 username，而是 "APP:zhangsan"
        if (principal instanceof UserDetails) {
            smartSubject = ((UserDetails) principal).getUsername();
        } else {
            smartSubject = principal.toString();
        }
        
        UserDetailsService userDetailsService = SpringBeanHolder.getBean(UserDetailsService.class);
        
        // 【核心改造 2】：将 "APP:zhangsan" 传给底层
        // Eladmin 的 @Cacheable 会以 "APP:zhangsan" 为 Key 存入 Redis，彻底绝缘了端冲突！
        UserDetails userDetails = userDetailsService.loadUserByUsername(smartSubject);
        
        return (JwtUserDto) userDetails;
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