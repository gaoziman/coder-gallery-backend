package org.leocoder.picture.domain.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07
 * @description : 用户密码更新请求
 */
@Data
@ApiModel(value = "UserUpdatePasswordRequest",description = "用户密码更新请求")
public class UserUpdatePasswordRequest {

    @ApiModelProperty(value = "旧密码", required = true, example = "oldPassword123")
    private String oldPassword;

    @ApiModelProperty(value = "新密码", required = true, example = "newPassword123")
    private String newPassword;

    @ApiModelProperty(value = "确认新密码", required = true, example = "newPassword123")
    private String checkPassword;
}