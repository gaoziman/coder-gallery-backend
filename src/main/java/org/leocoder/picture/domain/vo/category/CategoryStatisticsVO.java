package org.leocoder.picture.domain.vo.category;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

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

    @ApiModelProperty(value = "本月新增分类数")
    private Long newCategoriesOfMonth;

    @ApiModelProperty(value = "顶级分类数量")
    private Long topLevelCategories;

    @ApiModelProperty(value = "最大分类层级")
    private Integer maxCategoryLevel;

    @ApiModelProperty(value = "分类总增长率(与上月相比)")
    private Double categoryGrowthRate;

    @ApiModelProperty(value = "今日分类增长率")
    private Double todayCategoryGrowthRate;

    @ApiModelProperty(value = "本周分类增长率")
    private Double weeklyCategoryGrowthRate;

    @ApiModelProperty(value = "最多内容的分类ID")
    private Long mostContentsCategory;

    @ApiModelProperty(value = "最多内容的分类名称")
    private String mostContentsCategoryName;

    @ApiModelProperty(value = "最多内容的分类内容数")
    private Integer mostContentsCount;

    @ApiModelProperty(value = "激活状态的分类数量")
    private Long activeCategories;

    @ApiModelProperty(value = "禁用状态的分类数量")
    private Long disabledCategories;

    @ApiModelProperty(value = "内容总数量")
    private Long totalItems;

    @ApiModelProperty(value = "平均每个分类的内容数")
    private Double averageItemsPerCategory;

    @ApiModelProperty(value = "空分类数量(不包含任何内容)")
    private Long emptyCategoriesCount;

    @ApiModelProperty(value = "上月分类总数")
    private Long lastMonthTotalCategories;

    @ApiModelProperty(value = "各层级的分类数量分布")
    private Map<Integer, Long> levelDistribution;

    @ApiModelProperty(value = "平均子分类数")
    private Double averageChildrenCount;

    @ApiModelProperty(value = "最近一周活跃分类数")
    private Long activeLastWeekCategories;

    @ApiModelProperty(value = "最近一月活跃分类数")
    private Long activeLastMonthCategories;

    @ApiModelProperty(value = "最少使用的分类ID")
    private Long leastContentsCategory;

    @ApiModelProperty(value = "最少使用的分类名称")
    private String leastContentsCategoryName;

    @ApiModelProperty(value = "最少使用的分类内容数")
    private Integer leastContentsCount;

    @ApiModelProperty(value = "统计数据更新时间")
    private LocalDateTime statisticsUpdateTime;

    @ApiModelProperty(value = "分类使用率排名(前10个)")
    private Map<String, Integer> topUsedCategories;

    @ApiModelProperty(value = "分类深度分布占比")
    private Map<Integer, Double> depthDistributionPercentage;
}