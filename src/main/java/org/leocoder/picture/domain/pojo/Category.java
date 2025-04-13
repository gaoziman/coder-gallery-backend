package org.leocoder.picture.domain.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 13:59
 * @description : 分类实体类
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "分类表")
public class Category {
    @ApiModelProperty(value = "分类ID")
    private Long id;

    @ApiModelProperty(value = "分类名称")
    private String name;

    @ApiModelProperty(value = "父级分类ID")
    private Long parentId;

    @ApiModelProperty(value = "分类类型(product-产品分类,article-文章分类等)")
    private String type;

    @ApiModelProperty(value = "分类层级(1-一级,2-二级,3-三级)")
    private Integer level;

    @ApiModelProperty(value = "分类路径(格式:1,2,3表示从一级到三级的ID路径)")
    private String path;

    @ApiModelProperty(value = "分类描述")
    private String description;

    @ApiModelProperty(value = "分类图标")
    private String icon;

    @ApiModelProperty(value = "分类别名(用于URL优化)")
    private String urlName;

    @ApiModelProperty(value = "排序优先级(数值越小排序越靠前)")
    private Integer sortOrder;

    @ApiModelProperty(value = "包含内容数量")
    private Integer contentCount;

    @ApiModelProperty(value = "状态(active-已启用,inactive-未启用)")
    private String status;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "创建人ID")
    private Long createUser;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "更新人ID")
    private Long updateUser;

    @ApiModelProperty(value = "是否删除(0-未删除,1-已删除)")
    private Integer isDeleted;
}