package org.leocoder.picture.domain.dto.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 13:54
 * @description : 瀑布流图片请求参数
 */
@Data
@ApiModel(value = "PictureWaterfallRequest", description = "瀑布流图片请求参数")
public class PictureWaterfallRequest {

    @ApiModelProperty(value = "每页数量，默认30")
    private Integer pageSize = 30;

    @ApiModelProperty(value = "排序方式: newest(最新发布)/popular(最受欢迎)/mostViewed(最多浏览)/mostLiked(最多点赞)/mostCollected(最多收藏)")
    private String sortBy = "newest";

    @ApiModelProperty(value = "分类ID")
    private Long categoryId;

    @ApiModelProperty(value = "标签ID列表")
    private List<Long> tagIds;

    @ApiModelProperty(value = "图片格式，如jpg,png")
    private String format;

    @ApiModelProperty(value = "宽度下限")
    private Integer minWidth;

    @ApiModelProperty(value = "高度下限")
    private Integer minHeight;

    @ApiModelProperty(value = "上传者ID")
    private Long userId;

    @ApiModelProperty(value = "搜索关键词")
    private String keyword;
}