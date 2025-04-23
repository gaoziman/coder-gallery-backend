package org.leocoder.picture.domain.vo.tag;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 10:54
 * @description : 关联内容视图对象
 */
@Data
@ApiModel(description = "关联内容视图对象")
public class TagRelatedItemVO  implements Serializable {

    private static final long serialVersionUID = 1866928681722513578L;

    @ApiModelProperty(value = "内容ID", example = "1")
    private Long id;
    
    @ApiModelProperty(value = "内容类型", example = "picture")
    private String type;
    
    @ApiModelProperty(value = "内容标题", example = "风景照片")
    private String title;
    
    @ApiModelProperty(value = "内容描述", example = "美丽的湖泊风景")
    private String description;
    
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
    
    @ApiModelProperty(value = "缩略图URL", example = "https://example.com/thumbnail.jpg")
    private String thumbnailUrl;
    
    @ApiModelProperty(value = "创建人", example = "admin")
    private String creator;
}