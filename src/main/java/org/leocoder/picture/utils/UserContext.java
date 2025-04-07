package org.leocoder.picture.utils;

import org.leocoder.picture.domain.pojo.User;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 16:45
 * @description : 用户上下文工具类，用于在当前线程中保存和获取用户信息
 */
public class UserContext {
    
    // 使用ThreadLocal来存储当前线程的用户信息
    private static final ThreadLocal<User> USER_THREAD_LOCAL = new ThreadLocal<>();
    
    /**
     * 设置当前线程的用户信息
     *
     * @param user 用户信息
     */
    public static void setUser(User user) {
        USER_THREAD_LOCAL.set(user);
    }
    
    /**
     * 获取当前线程的用户信息
     *
     * @return 用户信息
     */
    public static User getUser() {
        return USER_THREAD_LOCAL.get();
    }
    
    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        User user = getUser();
        return user != null ? user.getId() : null;
    }
    
    /**
     * 清除当前线程的用户信息
     * 注意：在请求结束时应该调用此方法，避免内存泄漏
     */
    public static void clear() {
        USER_THREAD_LOCAL.remove();
    }
}