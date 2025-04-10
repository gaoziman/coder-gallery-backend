package org.leocoder.picture.domain.dto.tag;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 10:39
 * @description : 批量删除请求
 */
@Data
@ApiModel(description = "批量删除请求")
public class TagBatchDeleteRequest {
    
    @ApiModelProperty(value = "ID列表", required = true, example = "[1, 2, 3]")
    private List<Long> ids;
}