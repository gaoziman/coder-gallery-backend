package org.leocoder.picture.domain.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07
 * @description : 管理员重置用户密码请求
 */
@Data
@ApiModel(value = "AdminResetPasswordRequest", description = "管理员重置用户密码请求")
public class AdminResetPasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户ID", required = true)
    private Long userId;

    @ApiModelProperty(value = "新密码", required = true, example = "newPassword123")
    private String newPassword;

    @ApiModelProperty(value = "确认密码", required = true, example = "newPassword123")
    private String checkPassword;
}