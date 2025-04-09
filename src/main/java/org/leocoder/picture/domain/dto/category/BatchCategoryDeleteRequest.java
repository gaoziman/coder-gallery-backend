package org.leocoder.picture.domain.dto.category;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 批量删除请求
 */
@Data
@ApiModel(value = "BatchDeleteRequest", description = "批量删除请求")
public class BatchCategoryDeleteRequest {
    
    @ApiModelProperty(value = "ID列表", required = true)
    private List<Long> ids;
}