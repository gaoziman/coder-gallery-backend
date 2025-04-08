package org.leocoder.picture.domain.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07
 * @description : 用户信息更新请求
 */
@Data
@ApiModel(value = "UserUpdateRequest",description = "用户信息更新请求")
public class UserUpdateRequest {

    @ApiModelProperty(value = "用户名", example = "图库大师")
    private String username;

    @ApiModelProperty(value = "手机号", example = "13800138000")
    private String phone;

    @ApiModelProperty(value = "头像URL", example = "https://example.com/avatar.png")
    private String avatar;

    @ApiModelProperty(value = "用户简介", example = "热爱摄影的设计师")
    private String userProfile;
}