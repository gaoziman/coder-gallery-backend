package org.leocoder.picture.domain.dto.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 15:47
 * @description :  批量上传图片请求参数
 */
@Data
@ApiModel(value = "PictureUploadByBatchRequest", description = "批量上传图片请求参数")
public class PictureUploadByBatchRequest {

    @ApiModelProperty(value = "搜索词")
    private String searchText;

    @ApiModelProperty(value = "抓取数量")
    private Integer count = 10;

    @ApiModelProperty(value = "名称前缀")
    private String namePrefix;

    @ApiModelProperty(value = "搜索源")
    private String source;

    @ApiModelProperty(value = "分类ID")
    private Long categoryId;

}

