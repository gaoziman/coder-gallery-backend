package org.leocoder.picture.domain.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 13:35
 * @description : 用户登录请求参数
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    private String account;

    /**
     * 密码
     */
    private String password;
}

