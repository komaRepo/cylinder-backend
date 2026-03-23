package me.zhengjie.modules.maint.sys;


import me.zhengjie.modules.maint.domain.dto.UserInfoDTO;

/**
 * 线程级别的用户上下文容器
 */
public class UserContext {

    // 使用 ThreadLocal 存储当前线程的用户信息
    private static final ThreadLocal<UserInfoDTO> USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 存入用户信息 (拦截器中调用)
     */
    public static void set(UserInfoDTO userInfo) {
        USER_THREAD_LOCAL.set(userInfo);
    }

    /**
     * 获取用户信息 (Service层业务代码中调用)
     */
    public static UserInfoDTO get() {
        return USER_THREAD_LOCAL.get();
    }

    /**
     * 快捷获取当前登录用户 ID
     */
    public static Long getUserId() {
        UserInfoDTO user = get();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 快捷获取当前登录用户所属企业 ID
     */
    public static Long getCompanyId() {
        UserInfoDTO user = get();
        return user != null ? user.getCompanyId() : null;
    }
    
    /**
     * 快捷获取当前登录用户所属企业 Path (重要！用于层级权限穿透查询)
     */
    public static String getCompanyPath() {
        UserInfoDTO user = get();
        return user != null ? user.getCompanyPath() : null;
    }

    /**
     * 清除用户信息 (必须调用！防止 Tomcat 线程池复用导致内存泄漏和数据串号)
     */
    public static void remove() {
        USER_THREAD_LOCAL.remove();
    }
}