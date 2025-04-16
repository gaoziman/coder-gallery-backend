package org.leocoder.picture.domain.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07
 * @description : 用户创建请求
 */
@Data
@ApiModel(value = "AdminUserAddRequest", description = "用户创建请求参数")
public class AdminUserAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户账号", required = true, example = "testuser")
    private String account;

    @ApiModelProperty(value = "用户密码", required = true, example = "password123")
    private String password;

    @ApiModelProperty(value = "用户名", example = "张三")
    private String username;

    @ApiModelProperty(value = "用户手机号", example = "13800138000")
    private String phone;

    @ApiModelProperty(value = "用户简介", example = "这是我的个人简介")
    private String userProfile;

    @ApiModelProperty(value = "用户头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @ApiModelProperty(value = "用户角色", example = "user")
    private String role;

    @ApiModelProperty(value = "用户状态", example = "active")
    private String status;

    @ApiModelProperty(value = "用户备注", example = "系统管理员")
    private String remark;
}