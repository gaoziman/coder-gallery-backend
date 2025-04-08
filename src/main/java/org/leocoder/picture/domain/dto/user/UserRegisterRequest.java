package org.leocoder.picture.domain.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 14:07
 * @description : 用户注册请求对象
 */
@Data
@ApiModel(value = "UserRegisterRequest",description = "用户注册请求对象")
public class UserRegisterRequest {
    @ApiModelProperty(value = "用户名", required = true, example = "leocoder")
    private String account;

    @ApiModelProperty(value = "密码", required = true, example = "123456")
    private String password;

    @ApiModelProperty(value = "确认密码", required = true, example = "123456")
    private String checkPassword;
}
