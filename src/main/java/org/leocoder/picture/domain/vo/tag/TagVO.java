package org.leocoder.picture.domain.vo.tag;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 10:48
 * @description : 标签视图对象
 */
@Data
@ApiModel(description = "标签视图对象")
public class TagVO {
    
    @ApiModelProperty(value = "标签ID", example = "1")
    private Long id;
    
    @ApiModelProperty(value = "标签名称", example = "技术")
    private String name;
    
    @ApiModelProperty(value = "标签颜色(HEX格式)", example = "#1890FF")
    private String color;
    
    @ApiModelProperty(value = "标签描述", example = "技术相关内容标签")
    private String description;
    
    @ApiModelProperty(value = "状态(active-已启用,inactive-未启用)", example = "active")
    private String status;

    
    @ApiModelProperty(value = "引用次数", example = "10")
    private Integer referenceCount;
    
    @ApiModelProperty(value = "排序优先级", example = "100")
    private Integer sortOrder;
    
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
    
    @ApiModelProperty(value = "创建人", example = "admin")
    private String creator;
}