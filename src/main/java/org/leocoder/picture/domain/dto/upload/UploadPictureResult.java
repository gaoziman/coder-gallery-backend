package org.leocoder.picture.domain.dto.upload;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 00:20
 * @description :  接受图片解析信息
 */
@Data
@ApiModel(value = "上传图片结果")
public class UploadPictureResult {

    @ApiModelProperty(value = "图片地址")
    private String url;

    @ApiModelProperty(value = "缩略图 url")
    private String thumbnailUrl;

    @ApiModelProperty(value = "原始文件名")
    private String originalName;

    @ApiModelProperty(value = "图片名称")
    private String picName;

    @ApiModelProperty(value = "文件体积")
    private Long picSize;

    @ApiModelProperty(value = "图片宽度")
    private int picWidth;

    @ApiModelProperty(value = "图片高度")
    private int picHeight;

    @ApiModelProperty(value = "图片宽高比")
    private Double picScale;

    @ApiModelProperty(value = "图片格式")
    private String picFormat;

    @ApiModelProperty(value = "图片主色调")
    private String picColor;


}
