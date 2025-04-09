package org.leocoder.picture.domain.dto.category;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 分类更新请求
 */
@Data
@ApiModel(value = "CategoryUpdateRequest", description = "分类更新请求")
public class CategoryUpdateRequest {
    
    @ApiModelProperty(value = "分类ID", required = true)
    private Long id;
    
    @ApiModelProperty(value = "分类名称")
    private String name;
    
    @ApiModelProperty(value = "分类描述")
    private String description;
    
    @ApiModelProperty(value = "分类图标")
    private String icon;
    
    @ApiModelProperty(value = "URL名称")
    private String urlName;
    
    @ApiModelProperty(value = "排序")
    private Integer sortOrder;
    
    @ApiModelProperty(value = "状态")
    private String status;
}