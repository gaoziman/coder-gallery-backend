package org.leocoder.picture.domain.dto.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 23:00
 * @description :
 */
@Data
@ApiModel(value = "PictureEditRequest", description = "图片编辑请求对象")
public class PictureEditRequest {

    @ApiModelProperty(value = "id", required = true)
    private Long id;

    @ApiModelProperty(value = "图片名称", required = true)
    private String name;

    @ApiModelProperty(value = "简介", required = true)
    private String description;

    @ApiModelProperty(value = "分类Id", required = true)
    private Long categoryId;

    @ApiModelProperty(value = "标签IDs", required = true)
    private List<Long> tagIds;

}
