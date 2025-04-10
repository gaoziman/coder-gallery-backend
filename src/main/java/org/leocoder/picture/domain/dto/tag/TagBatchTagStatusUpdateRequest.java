package org.leocoder.picture.domain.dto.tag;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 10:37
 * @description : 批量标签状态更新请求
 */
@Data
@ApiModel(description = "批量标签状态更新请求")
public class TagBatchTagStatusUpdateRequest {
    
    @ApiModelProperty(value = "标签ID列表", required = true, example = "[1, 2, 3]")
    private List<Long> ids;
    
    @ApiModelProperty(value = "状态(active-已启用,inactive-未启用)", required = true, example = "active")
    private String status;
}