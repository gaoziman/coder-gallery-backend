package org.leocoder.picture.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.leocoder.picture.domain.dto.user.AdminUserQueryRequest;
import org.leocoder.picture.domain.pojo.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 13:26
 * @description : 用户Mapper接口
 */
@Mapper
public interface UserMapper {
    int deleteByPrimaryKey(Long id);

    int insert(User record);

    int insertSelective(User record);

    User selectById(Long id);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);

    /**
     * 根据账号查询用户
     *
     * @param account 账号
     * @return 用户对象
     */
    User selectByAccount(@Param("account") String account);

    /**
     * 查询所有用户账号
     *
     * @return 用户列表
     */
    List<User> selectAllAccounts();


    /**
     * 更新用户登录信息
     *
     * @param id            用户id
     * @param lastLoginTime 登录时间
     * @param lastLoginIp   登录ip
     */
    void updateLoginInfo(@Param("id") Long id,
                         @Param("lastLoginTime") LocalDateTime lastLoginTime,
                         @Param("lastLoginIp") String lastLoginIp);


    /**
     * 使用指定ID插入用户
     *
     * @param user 用户对象
     * @return 影响的行数
     */
    int insertWithId(User user);


    /**
     * 根据ID更新用户信息
     *
     * @param user 用户对象
     * @return 影响行数
     */
    int updateById(User user);

    /**
     * 更新用户密码
     *
     * @param userId     用户ID
     * @param password   加密后的密码
     * @param salt       新的盐值
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updatePassword(@Param("userId") Long userId,
                       @Param("password") String password,
                       @Param("salt") String salt,
                       @Param("updateTime") LocalDateTime updateTime);


    /**
     * 根据条件统计用户数量
     *
     * @param queryUser         查询条件
     * @param registerTimeStart 注册时间开始
     * @param registerTimeEnd   注册时间结束
     * @return 用户数量
     */
    Long countUsers(@Param("queryUser") AdminUserQueryRequest queryUser,
                    @Param("registerTimeStart") LocalDateTime registerTimeStart,
                    @Param("registerTimeEnd") LocalDateTime registerTimeEnd);

    /**
     * 分页查询用户列表
     *
     * @param queryUser         查询条件
     * @param registerTimeStart 注册时间开始
     * @param registerTimeEnd   注册时间结束
     * @param offset            偏移量
     * @param pageSize          页面大小
     * @return 用户列表
     */
    List<User> listUsersByPage(@Param("queryUser") AdminUserQueryRequest queryUser,
                               @Param("registerTimeStart") LocalDateTime registerTimeStart,
                               @Param("registerTimeEnd") LocalDateTime registerTimeEnd,
                               @Param("offset") Integer offset,
                               @Param("pageSize") Integer pageSize);

    /**
     * 逻辑删除用户
     *
     * @param id         用户ID
     * @param updateTime 更新时间
     * @param updateBy   更新人
     * @return 影响行数
     */
    int logicDeleteUser(@Param("id") Long id,
                        @Param("updateTime") LocalDateTime updateTime,
                        @Param("updateBy") Long updateBy);

    /**
     * 更新用户状态
     *
     * @param id         用户ID
     * @param status     用户状态
     * @param updateTime 更新时间
     * @param updateBy   更新人
     * @return 影响行数
     */
    int updateUserStatus(@Param("id") Long id,
                         @Param("status") String status,
                         @Param("updateTime") LocalDateTime updateTime,
                         @Param("updateBy") Long updateBy);


    /**
     * 统计注册用户总数
     *
     * @return 用户总数
     */
    Long countTotalUsers();

    /**
     * 统计本月新增用户数
     *
     * @return 本月新增用户数
     */
    Long countNewUsersThisMonth();

    /**
     * 根据角色统计用户数
     *
     * @param role 用户角色
     * @return 该角色的用户数
     */
    Long countUsersByRole(@Param("role") String role);

    /**
     * 统计活跃用户数(最近30天有登录记录)
     *
     * @return 活跃用户数
     */
    Long countActiveUsers();


    /**
     * 统计上月用户总数
     *
     * @return 上月用户总数
     */
    Long countLastMonthTotalUsers();

    /**
     * 统计上月新增用户数
     *
     * @return 上月新增用户数
     */
    Long countNewUsersLastMonth();

    /**
     * 统计上月VIP用户数
     *
     * @param role VIP角色值
     * @return 上月VIP用户数
     */
    Long countLastMonthUsersByRole(@Param("role") String role);

    /**
     * 统计上月活跃用户比例
     *
     * @return 上月活跃用户数
     */
    Long countLastMonthActiveUsers();

    /**
     * 统计今日登录用户数量
     *
     * @return 今日登录用户数量
     */
    Long countTodayLoginUsers();

    /**
     * 统计本周新增用户数量
     *
     * @return 本周新增用户数量
     */
    Long countNewUsersThisWeek();

    /**
     * 统计被冻结的账户数量
     *
     * @param status 被冻结状态值
     * @return 冻结账户数量
     */
    Long countUsersByStatus(@Param("status") String status);


    /**
     * 批量逻辑删除用户
     *
     * @param ids        用户ID列表
     * @param updateTime 更新时间
     * @param updateBy   更新人
     * @return 影响行数
     */
    int batchLogicDeleteUsers(@Param("ids") List<Long> ids,
                              @Param("updateTime") LocalDateTime updateTime,
                              @Param("updateBy") Long updateBy);


    /**
     * 根据id获取用户
     *
     * @param createUser 创建人ID
     * @return 用户
     */
    User getUsernameById(Long createUser);

    /**
     * 查询用户列表（分页由 PageHelper 处理）
     *
     * @param queryUser 查询条件
     * @param registerTimeStart 注册时间开始
     * @param registerTimeEnd 注册时间结束
     * @return 用户列表
     */
    List<User> listUsers(@Param("queryUser") AdminUserQueryRequest queryUser,
                         @Param("registerTimeStart") String registerTimeStart,
                         @Param("registerTimeEnd") String registerTimeEnd);;
}