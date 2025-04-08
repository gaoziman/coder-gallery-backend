package org.leocoder.picture.domain.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 20:59
 * @description : 管理员用户查询请求
 */
@Data
@ApiModel(value = "AdminUserQueryRequest", description = "管理员用户查询请求参数")
public class AdminUserQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户ID")
    private Long id;

    @ApiModelProperty(value = "用户账号")
    private String account;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "用户手机号")
    private String phone;

    @ApiModelProperty(value = "用户角色")
    private String role;

    @ApiModelProperty(value = "用户状态")
    private String status;

    @ApiModelProperty(value = "注册时间范围-开始")
    private LocalDateTime registerTimeStart;

    @ApiModelProperty(value = "注册时间范围-结束")
    private LocalDateTime registerTimeEnd;

    @ApiModelProperty(value = "当前页码", example = "1")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "每页记录数", example = "10")
    private Integer pageSize = 10;
}