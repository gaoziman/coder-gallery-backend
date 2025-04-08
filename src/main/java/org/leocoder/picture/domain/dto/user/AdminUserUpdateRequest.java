package org.leocoder.picture.domain.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 21:01
 * @description :
 */
@Data
@ApiModel(value = "AdminUserUpdateRequest", description = "用户注册请求对象")
public class AdminUserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户ID", required = true)
    private Long id;

    @ApiModelProperty(value = "用户名", example = "张三")
    private String username;

    @ApiModelProperty(value = "用户手机号", example = "13800138000")
    private String phone;

    @ApiModelProperty(value = "用户头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @ApiModelProperty(value = "用户角色", example = "user")
    private String role;

    @ApiModelProperty(value = "用户状态", example = "active")
    private String status;

    @ApiModelProperty(value = "备注", example = "xxx")
    private String remark;
}
