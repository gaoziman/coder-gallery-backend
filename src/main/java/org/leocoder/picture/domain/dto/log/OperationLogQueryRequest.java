package org.leocoder.picture.domain.dto.log;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.leocoder.picture.common.PageRequest;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 16:00
 * @description : 操作日志查询条件
 */
@Data
@ApiModel(value = "OperationLogQueryRequest", description = "操作日志查询条件")
public class OperationLogQueryRequest extends PageRequest {

    @ApiModelProperty(value = "操作模块", example = "picture")
    private String module;

    @ApiModelProperty(value = "操作类型", example = "upload")
    private String action;

    @ApiModelProperty(value = "操作状态", example = "true")
    private Integer status;

    @ApiModelProperty(value = "创建开始时间")
    private String createTimeStart;

    @ApiModelProperty(value = "创建结束时间")
    private String createTimeEnd;


    @ApiModelProperty(value = "搜索内容", example = "张三")
    private String searchContent;


    @ApiModelProperty(value = "是否导出全部", example = "true")
    private Boolean exportAll;
}