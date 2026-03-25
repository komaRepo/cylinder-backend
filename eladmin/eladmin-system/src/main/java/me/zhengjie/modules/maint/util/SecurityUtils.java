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
        
        // 1. 安全提取出 username (此时它只是原生的 Spring User)
        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        
        // 2. 动态获取 Eladmin 的 UserDetailsService
        UserDetailsService userDetailsService = SpringBeanHolder.getBean(UserDetailsService.class);
        
        // 3. 重新加载完整的用户信息
        // 💡 这里的神妙之处：Eladmin 底层会直接命中 Redis 缓存，
        // 返回的正是我们之前在 loadUserByUsername 里塞满了 companyId 的那个 JwtUserDto！
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        
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