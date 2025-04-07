package org.leocoder.picture.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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
}