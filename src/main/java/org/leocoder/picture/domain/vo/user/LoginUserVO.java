package org.leocoder.picture.domain.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(description = "脱敏后登录用户信息")
public class LoginUserVO implements Serializable {
    private static final long serialVersionUID = 6885794651651821457L;

    @ApiModelProperty(value = "用户 id")
    private Long id;

    @ApiModelProperty(value = "账号")
    private String account;

    @ApiModelProperty(value = "用户昵称")
    private String username;

    @ApiModelProperty(value = "用户手机号")
    private String userPhone;

    @ApiModelProperty(value = "用户头像")
    private String avatar;

    @ApiModelProperty(value = "用户简介")
    private String userProfile;


    @ApiModelProperty(value = "用户角色")
    private String role;


    @ApiModelProperty(value = "Sa-Token令牌")
    private String tokenValue;

    @ApiModelProperty(value = "Sa-Token令牌名称")
    private String tokenName;

    @ApiModelProperty(value = "令牌过期时间（秒）")
    private Long tokenTimeout;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;


    @ApiModelProperty(value = "最后登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    @ApiModelProperty(value = "最后登录IP")
    private String lastLoginIp;

}