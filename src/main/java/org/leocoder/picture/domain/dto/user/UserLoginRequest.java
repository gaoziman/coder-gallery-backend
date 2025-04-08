package org.leocoder.picture.domain.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 13:35
 * @description : 用户登录请求参数
 */
@Data
@ApiModel(value = "UserLoginRequest",description = "用户登录请求参数")
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    @ApiModelProperty(value = "用户名", required = true, example = "leocoder")
    private String account;

    @ApiModelProperty(value = "密码", required = true, example = "123456")
    private String password;
}

