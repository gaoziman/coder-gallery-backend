package org.leocoder.picture.domain.dto.user;

import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 14:07
 * @description : 用户注册请求对象
 */
@Data
public class UserRegisterRequest {
    private String account;

    private String password;

    private String checkPassword;
}
