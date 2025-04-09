package org.leocoder.picture.domain.dto.category;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 批量分类移动请求
 */
@Data
@ApiModel(value = "BatchCategoryMoveRequest", description = "批量分类移动请求")
public class BatchCategoryMoveRequest {
    
    @ApiModelProperty(value = "分类ID列表", required = true)
    private List<Long> ids;

    @ApiModelProperty(value = "新的父分类ID", required = true)
    private Long parentId;
}