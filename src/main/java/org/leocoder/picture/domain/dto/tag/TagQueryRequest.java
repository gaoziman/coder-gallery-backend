package org.leocoder.picture.domain.dto.tag;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.leocoder.picture.common.PageRequest;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 10:41
 * @description : 标签查询请求
 */
@Data
@ApiModel(description = "标签查询请求")
public class TagQueryRequest extends PageRequest {
    
    @ApiModelProperty(value = "标签名称(模糊查询)", example = "技术")
    private String name;
    
    @ApiModelProperty(value = "状态(active-已启用,inactive-未启用)", example = "active")
    private String status;

    @ApiModelProperty(value = "创建开始时间", example = "2025-01-01")
    private String createTimeStart;

    @ApiModelProperty(value = "创建结束时间", example = "2025-12-31")
    private String createTimeEnd;

}