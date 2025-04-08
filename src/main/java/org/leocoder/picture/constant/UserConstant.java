package org.leocoder.picture.constant;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07
 * @description : 用户常量
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 普通用户角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 默认状态 - 正常
     */
    String STATUS_NORMAL = "active";

    /**
     * 状态 - 禁用
     */
    String STATUS_DISABLED = "banned";
}