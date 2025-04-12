package org.leocoder.picture.domain.dto.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 13:54
 * @description : 图片上传请求对象
 */
@Data
@ApiModel(value = "PictureUploadRequest", description = "图片上传请求对象")
public class PictureUploadRequest{

    @ApiModelProperty(value = "图片 id（用于修改）", required = false)
    private Long id;

    @ApiModelProperty(value = "图片 url", required = true)
    private String fileUrl;

    @ApiModelProperty(value = "图片名称", required = true)
    private String picName;

    @ApiModelProperty(value = "空间 id", required = true)
    private Long spaceId;

}

