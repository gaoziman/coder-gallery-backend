package org.leocoder.picture.service;

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
}
