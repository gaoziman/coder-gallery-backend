package org.leocoder.picture.domain.dto.log;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.leocoder.picture.common.PageRequest;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 16:05
 * @description : 登录日志查询条件
 */
@Data
@ApiModel(value = "LoginLogQueryRequest", description = "登录日志查询条件")
public class LoginLogQueryRequest extends PageRequest {

    @ApiModelProperty(value = "用户ID", example = "1")
    private Long createBy;

    @ApiModelProperty(value = "登录状态", example = "1")
    private Integer status;

    @ApiModelProperty(value = "创建开始时间", example = "2021-01-01 00:00:00")
    private String createTimeStart;

    @ApiModelProperty(value = "创建结束时间", example = "2021-12-31 23:59:59")
    private String createTimeEnd;


    @ApiModelProperty(value = "操作系统", example = "Windows")
    private String os;

    @ApiModelProperty(value = "浏览器", example = "Chrome")
    private String browser;

    @ApiModelProperty(value = "搜索内容（模糊匹配IP、位置、设备等）", example = "192.168.1.1")
    private String searchContent;


    @ApiModelProperty(value = "是否导出全部", example = "true")
    private Boolean exportAll;
}