package org.leocoder.picture.domain.dto.tag;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 10:35
 * @description : 标签状态更新请求
 */
@Data
@ApiModel(description = "标签状态更新请求")
public class TagStatusUpdateRequest {
    
    @ApiModelProperty(value = "标签ID", required = true, example = "1")
    private Long id;
    
    @ApiModelProperty(value = "状态(active-已启用,inactive-未启用)", required = true, example = "active")
    private String status;
}