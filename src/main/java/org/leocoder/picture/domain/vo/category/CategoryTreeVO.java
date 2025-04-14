package org.leocoder.picture.domain.vo.category;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 分类树形结构VO
 */
@Data
@Builder
@ApiModel(description = "分类树形结构VO")
public class CategoryTreeVO {
    
    @ApiModelProperty(value = "分类ID")
    private Long id;
    
    @ApiModelProperty(value = "分类名称")
    private String name;
    
    @ApiModelProperty(value = "父分类ID")
    private Long parentId;
    
    @ApiModelProperty(value = "分类类型")
    private String type;
    
    @ApiModelProperty(value = "层级")
    private Integer level;
    
    @ApiModelProperty(value = "路径")
    private String path;
    
    @ApiModelProperty(value = "分类描述")
    private String description;
    
    @ApiModelProperty(value = "分类图标")
    private String icon;
    
    @ApiModelProperty(value = "URL名称")
    private String urlName;
    
    @ApiModelProperty(value = "排序")
    private Integer sortOrder;
    
    @ApiModelProperty(value = "内容数量")
    private Integer contentCount;
    
    @ApiModelProperty(value = "状态")
    private String status;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
    
    @ApiModelProperty(value = "子分类列表")
    private List<CategoryTreeVO> children;
}