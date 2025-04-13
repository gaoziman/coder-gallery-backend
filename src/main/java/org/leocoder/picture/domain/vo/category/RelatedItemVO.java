package org.leocoder.picture.domain.vo.category;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 分类关联项VO
 */
@Data
@Builder
@ApiModel(description = "分类关联项VO")
public class RelatedItemVO {
    
    @ApiModelProperty(value = "关联项ID")
    private Long id;
    
    @ApiModelProperty(value = "内容ID")
    private Long contentId;
    
    @ApiModelProperty(value = "内容类型")
    private String contentType;
    
    @ApiModelProperty(value = "内容标题")
    private String title;
    
    @ApiModelProperty(value = "内容描述")
    private String description;
    
    @ApiModelProperty(value = "内容缩略图")
    private String thumbnail;
    
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
    
    @ApiModelProperty(value = "创建用户")
    private Long createUser;
    
    @ApiModelProperty(value = "创建用户名")
    private String createUsername;
    
    @ApiModelProperty(value = "内容状态")
    private String status;
}