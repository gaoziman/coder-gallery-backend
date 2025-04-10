package org.leocoder.picture.service;

import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.domain.dto.user.AdminUserAddRequest;
import org.leocoder.picture.domain.dto.user.AdminUserQueryRequest;
import org.leocoder.picture.domain.dto.user.AdminUserUpdateRequest;
import org.leocoder.picture.domain.dto.user.UserUpdateRequest;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.user.LoginUserVO;
import org.leocoder.picture.domain.vo.user.UserStatisticsVO;
import org.leocoder.picture.domain.vo.user.UserVO;

import java.util.List;

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
     * @param oldPassword   旧密码
     * @param newPassword   新密码
     * @param checkPassword 确认密码
     * @return 是否成功
     */
    boolean updateUserPassword(String oldPassword, String newPassword, String checkPassword);

    /**
     * 管理员添加用户
     *
     * @param userAddRequest 用户添加请求
     * @return 新创建的用户ID
     */
    Long addUser(AdminUserAddRequest userAddRequest);

    /**
     * 根据ID获取用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    UserVO getUserById(Long id);

    /**
     * 根据ID获取脱敏用户信息
     *
     * @param id 用户ID
     * @return 脱敏用户信息
     */
    UserVO getUserVOById(Long id);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否成功
     */
    Boolean deleteUser(Long id);

    /**
     * 管理员更新用户信息
     *
     * @param userUpdateRequest 用户更新请求
     * @return 是否成功
     */
    Boolean updateUserByAdmin(AdminUserUpdateRequest userUpdateRequest);

    /**
     * 分页获取用户列表
     *
     * @param userQueryRequest 用户查询请求
     * @return 分页用户列表
     */
    PageResult<UserVO> listUserByPage(AdminUserQueryRequest userQueryRequest);

    /**
     * 禁用用户
     *
     * @param id 用户ID
     * @return 是否成功
     */
    Boolean banUser(Long id);

    /**
     * 解禁用户
     *
     * @param id 用户ID
     * @return 是否成功
     */
    Boolean unbanUser(Long id);

    /**
     * 获取用户统计信息
     *
     * @return 用户统计信息
     */
    UserStatisticsVO getUserStatistics();


    /**
     * 管理员重置用户密码
     *
     * @param userId        用户ID
     * @param newPassword   新密码
     * @param checkPassword 确认密码
     * @return 是否成功
     */
    Boolean resetUserPassword(Long userId, String newPassword, String checkPassword);

    /**
     * 批量删除用户
     *
     * @param ids 用户ID列表
     * @return 是否成功
     */
    Boolean batchDeleteUsers(List<Long> ids);


    /**
     * 根据用户ID获取用户名
     *
     * @param createUser 用户ID
     * @return 用户名
     */
    String getUsernameById(Long createUser);
}
