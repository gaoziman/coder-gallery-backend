package org.leocoder.picture.domain.vo.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 20:12
 * @description : 瀑布流图片响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "PictureWaterfallVO", description = "瀑布流图片响应对象")
public class PictureWaterfallVO {

    @ApiModelProperty(value = "图片列表")
    private List<PictureVO> records;

    @ApiModelProperty(value = "是否有更多图片")
    private Boolean hasMore;

    @ApiModelProperty(value = "最后一张图片ID，用于加载更多")
    private Long lastId;

    @ApiModelProperty(value = "最后一张图片的排序值")
    private Object lastValue;

    @ApiModelProperty(value = "总记录数")
    private Long total;
}