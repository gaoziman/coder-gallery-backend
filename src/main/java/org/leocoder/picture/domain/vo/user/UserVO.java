package org.leocoder.picture.domain.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 17:05
 * @description : 用户信息视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "UserVO", description = "用户信息视图对象")
public class UserVO implements Serializable {
    private static final long serialVersionUID = 4388903113721497948L;

    @ApiModelProperty(value = "用户ID")
    private Long id;

    @ApiModelProperty(value = "账户名")
    private String account;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "头像URL")
    private String avatar;

    @ApiModelProperty(value = "个人简介")
    private String userProfile;


    @ApiModelProperty(value = "角色(admin-管理员,user-普通用户,superAdmin-超级管理员)")
    private String role;

    @ApiModelProperty(value = "状态(active-已激活,inactive-未激活,disabled-已禁用)")
    private String status;

    @ApiModelProperty(value = "最后登录IP")
    private String lastLoginIp;


    @ApiModelProperty(value = "最后登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    @ApiModelProperty(value = "注册时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime registerTime;


    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;


    @ApiModelProperty(value = "备注")
    private String remark;
}