package org.leocoder.picture.domain.vo.tag;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 10:59
 * @description : 标签分类视图对象
 */
@Data
@ApiModel(description = "标签分类视图对象")
public class TagCategoryVO {
    
    @ApiModelProperty(value = "分类名称", example = "技术")
    private String name;
    
    @ApiModelProperty(value = "标签数量", example = "30")
    private Integer count;
}