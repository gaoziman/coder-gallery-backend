package org.leocoder.picture.domain.dto.tag;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 10:30
 * @description : 标签创建请求
 */
@Data
@ApiModel(description = "标签创建请求")
public class TagCreateRequest {
    
    @ApiModelProperty(value = "标签名称", required = true, example = "技术")
    private String name;
    
    @ApiModelProperty(value = "标签颜色(HEX格式)", example = "#1890FF")
    private String color;
    
    @ApiModelProperty(value = "标签描述", example = "技术相关内容标签")
    private String description;

    @ApiModelProperty(value = "标签状态", example = "启用")
    private String status;
    
    @ApiModelProperty(value = "排序优先级", example = "100")
    private Integer sortOrder = 100;
}