package org.leocoder.picture.utils;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 15:02
 * @description : 安全工具类
 */
@Slf4j
public class SecurityUtils {

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        if (!StpUtil.isLogin()) {
            return null;
        }

        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            log.error("获取当前登录用户ID异常", e);
            return null;
        }
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户对象
     */
    public static User getCurrentUser() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "用户未登录");
        }

        // 从用户上下文中获取用户信息
        User user = UserContext.getUser();
        if (user == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取用户信息失败");
        }

        return user;
    }

    /**
     * 判断当前用户是否为管理员
     *
     * @return 是否为管理员
     */
    public static boolean isAdmin() {
        if (!StpUtil.isLogin()) {
            return false;
        }

        User user = UserContext.getUser();
        return user != null && "admin".equalsIgnoreCase(user.getRole());
    }
}