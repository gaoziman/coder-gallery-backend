package org.leocoder.picture.domain.dto.category;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.leocoder.picture.common.PageRequest;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 分类查询请求
 */
@Data
@ApiModel(value = "CategoryQueryRequest",description = "分类查询请求")
public class CategoryQueryRequest extends PageRequest {

    @ApiModelProperty(value = "分类名称")
    private String name;

    @ApiModelProperty(value = "父分类ID")
    private Long parentId;

    @ApiModelProperty(value = "分类类型")
    private String type;

    @ApiModelProperty(value = "层级")
    private Integer level;

    @ApiModelProperty(value = "状态")
    private String status;

    @ApiModelProperty(value = "创建开始时间")
    private String createTimeStart;

    @ApiModelProperty(value = "创建结束时间")
    private String createTimeEnd;
}