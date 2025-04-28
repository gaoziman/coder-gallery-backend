package org.leocoder.picture.domain.vo.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-26 09:30
 * @description : 图片统计数据VO
 */
@Data
@Builder
@ApiModel(value = "PictureStatisticsVO", description = "图片统计数据")
public class PictureStatisticsVO implements Serializable {

    private static final long serialVersionUID = 497063464420292794L;

    @ApiModelProperty(value = "今日上传数量")
    private Long todayUploadCount;

    @ApiModelProperty(value = "本周上传数量")
    private Long weekUploadCount;

    @ApiModelProperty(value = "本周上传环比增长率", example = "15.2")
    private Double weekUploadGrowthRate;

    @ApiModelProperty(value = "本月上传数量")
    private Long monthUploadCount;

    @ApiModelProperty(value = "本月上传环比增长率", example = "8.7")
    private Double monthUploadGrowthRate;

    @ApiModelProperty(value = "待审核图片数量")
    private Long pendingReviewCount;

    @ApiModelProperty(value = "图片总数")
    private Long totalCount;

    @ApiModelProperty(value = "总浏览量")
    private Long totalViewCount;

    @ApiModelProperty(value = "浏览量环比增长率", example = "12.6")
    private Double viewCountGrowthRate;

    @ApiModelProperty(value = "总点赞量")
    private Long totalLikeCount;

    @ApiModelProperty(value = "点赞量环比增长率", example = "7.3")
    private Double likeCountGrowthRate;
}