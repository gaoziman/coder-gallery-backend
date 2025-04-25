package org.leocoder.picture.domain.dto.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.leocoder.picture.common.PageRequest;

@Data
@ApiModel(value = "ImageSearchRequest", description = "以图搜图请求")
public class ImageSearchRequest extends PageRequest {

    @ApiModelProperty(value = "图片ID", required = true, example = "1")
    private Long pictureId;

    @ApiModelProperty(value = "搜索来源", example = "all")
    private String source = "all";

    @ApiModelProperty(value = "是否包含同一用户图片", example = "false")
    private Boolean includeSameUser = false;

    @ApiModelProperty(value = "是否保存搜索结果", example = "false")
    private Boolean saveResults = false;

    @ApiModelProperty(value = "是否是再次搜索", example = "false")
    private Boolean isResearch = false;

    @ApiModelProperty(value = "自定义搜索关键词", example = "美丽风景")
    private String customKeyword;

    public ImageSearchRequest() {
        // 设置默认查询6条记录
        super.setPageSize(8);
    }
}