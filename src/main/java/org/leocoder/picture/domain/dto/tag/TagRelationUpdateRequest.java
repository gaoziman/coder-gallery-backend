package org.leocoder.picture.domain.dto.tag;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 10:45
 * @description : 更新内容标签关系请求
 */
@Data
@ApiModel(description = "更新内容标签关系请求")
public class TagRelationUpdateRequest {
    
    @ApiModelProperty(value = "标签ID列表", example = "[1, 2, 3]")
    private List<Long> tagIds;
    
    @ApiModelProperty(value = "内容类型", required = true, example = "picture")
    private String contentType;
    
    @ApiModelProperty(value = "内容ID", required = true, example = "1")
    private Long contentId;
}