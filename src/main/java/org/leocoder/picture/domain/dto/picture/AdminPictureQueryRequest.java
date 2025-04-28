package org.leocoder.picture.domain.dto.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.leocoder.picture.common.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-25 08:40
 * @description : 后台图片查询请求
 */
@Data
@ApiModel(value = "AdminPictureQueryRequest", description = "后台图片查询请求")
public class AdminPictureQueryRequest extends PageRequest {

    @ApiModelProperty(value = "图片ID")
    private Long id;

    @ApiModelProperty(value = "图片名称")
    private String name;

    @ApiModelProperty(value = "上传者ID")
    private Long userId;

    @ApiModelProperty(value = "原始文件名")
    private String originalName;

    @ApiModelProperty(value = "图片格式")
    private String format;

    @ApiModelProperty(value = "分类ID")
    private Long categoryId;

    @ApiModelProperty(value = "标签ID列表")
    private List<Long> tagIds;

    @ApiModelProperty(value = "审核状态")
    private Integer reviewStatus;

    @ApiModelProperty(value = "创建时间起始")
    private LocalDateTime createTimeStart;

    @ApiModelProperty(value = "创建时间结束")
    private LocalDateTime createTimeEnd;

    @ApiModelProperty(value = "排序字段")
    private String sortField;

    @ApiModelProperty(value = "排序方式(asc/desc)")
    private String sortOrder;
}