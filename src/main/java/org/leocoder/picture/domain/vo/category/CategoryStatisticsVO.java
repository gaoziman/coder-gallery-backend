package org.leocoder.picture.domain.vo.category;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 分类统计VO
 */
@Data
@Builder
@ApiModel(description = "分类统计VO")
public class CategoryStatisticsVO implements Serializable {
    private static final long serialVersionUID = 878049675493665235L;

    @ApiModelProperty(value = "分类总数")
    private Long totalCategories;

    @ApiModelProperty(value = "今日新增分类数")
    private Long newCategoriesOfToday;

    @ApiModelProperty(value = "本周新增分类数")
    private Long newCategoriesOfWeek;

    @ApiModelProperty(value = "本周新增分类环比增长率", example = "5.2")
    private Double weekGrowthRate;

    @ApiModelProperty(value = "本月新增分类数")
    private Long newCategoriesOfMonth;

    @ApiModelProperty(value = "本月新增分类环比增长率", example = "12.5")
    private Double monthGrowthRate;

    @ApiModelProperty(value = "顶级分类数量")
    private Long topLevelCategories;

    @ApiModelProperty(value = "顶级分类环比增长率", example = "3.8")
    private Double topLevelGrowthRate;

    @ApiModelProperty(value = "空分类数量(不包含任何内容)")
    private Long emptyCategoriesCount;

    @ApiModelProperty(value = "空分类环比增长率", example = "-8.3")
    private Double emptyCategoriesGrowthRate;

    @ApiModelProperty(value = "使用分类的图片总数")
    private Long totalItems;

    @ApiModelProperty(value = "使用分类图片总数环比增长率", example = "15.7")
    private Double totalItemsGrowthRate;
}