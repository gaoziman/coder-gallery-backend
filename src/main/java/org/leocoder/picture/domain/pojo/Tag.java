package org.leocoder.picture.domain.pojo;

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
 * @date 2025-04-09 10:23
 * @description : 标签实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "标签表")
public class Tag {

    @ApiModelProperty("标签ID")
    private Long id;

    @ApiModelProperty("标签名称")
    private String name;

    @ApiModelProperty("标签颜色(HEX格式，如#1890FF)")
    private String color;

    @ApiModelProperty("标签描述")
    private String description;

    @ApiModelProperty("状态(active-已启用,inactive-未启用)")
    private String status;

    @ApiModelProperty("引用次数")
    private Integer referenceCount;

    @ApiModelProperty("排序优先级(数值越小排序越靠前)")
    private Integer sortOrder;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("创建人ID")
    private Long createUser;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("更新人ID")
    private Long updateUser;

    @ApiModelProperty("是否删除(0-未删除,1-已删除)")
    private Integer isDeleted;
}