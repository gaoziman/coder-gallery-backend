package org.leocoder.picture.service;

import org.leocoder.picture.domain.dto.user.UserUpdateRequest;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.user.LoginUserVO;
import org.leocoder.picture.domain.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 13:26
 * @description :
 */

public interface UserService {


    /**
     * 根据账号查询用户
     *
     * @param account 账号
     * @return 用户对象
     */
    User getUserByAccount(String account);

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 用户id
     */
    Long userRegister(String userAccount, String userPassword, String checkPassword);


    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @return 登录成功的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword);


    /**
     * 获取当前登录用户信息
     *
     * @return 当前登录用户信息
     */
    UserVO getCurrentUser();



    /**
     * 用户注销
     *
     * @return 是否成功
     */
    boolean userLogout();

    /**
     * 更新用户信息
     *
     * @param userUpdateRequest 用户信息更新请求
     * @return 是否成功
     */
    boolean updateUserInfo(UserUpdateRequest userUpdateRequest);

    /**
     * 更新用户密码
     *
     * @param oldPassword    旧密码
     * @param newPassword    新密码
     * @param checkPassword  确认密码
     * @return 是否成功
     */
    boolean updateUserPassword(String oldPassword, String newPassword, String checkPassword);
}
