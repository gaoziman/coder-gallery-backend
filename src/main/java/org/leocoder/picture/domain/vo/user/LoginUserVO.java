package org.leocoder.picture.domain.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 13:55
 * @description : 脱敏后登录用户信息
 */
@Data
@Builder
public class LoginUserVO implements Serializable {

    /**
     * 用户 id
     */
    private Long id;

    /**
     * 账号
     */
    private String account;

    /**
     * 用户昵称
     */
    private String username;


    /**
     * 用户手机号
     */
    private String userPhone;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 角色(admin-管理员,user-普通用户)
     */
    private String role;


    /**
     * Sa-Token令牌
     */
    private String tokenValue;

    /**
     * Sa-Token令牌名称
     */
    private String tokenName;

    /**
     * 令牌过期时间（秒）
     */
    private Long tokenTimeout;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;


    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    private static final long serialVersionUID = 1L;
}