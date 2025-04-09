package org.leocoder.picture.domain.dto.category;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 分类移动请求
 */
@Data
@ApiModel(value = "CategoryMoveRequest", description = "分类移动请求")
public class CategoryMoveRequest {
    
    @ApiModelProperty(value = "分类ID", required = true)
    private Long id;
    
    @ApiModelProperty(value = "新的父分类ID", required = true)
    private Long parentId;
}