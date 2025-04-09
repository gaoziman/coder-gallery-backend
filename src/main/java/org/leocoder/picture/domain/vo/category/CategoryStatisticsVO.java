package org.leocoder.picture.domain.vo.category;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 分类统计VO
 */
@Data
@Builder
@ApiModel(value = "CategoryStatisticsVO", description = "分类统计VO")
public class CategoryStatisticsVO {
    
    @ApiModelProperty(value = "分类总数")
    private Long totalCategories;
    
    @ApiModelProperty(value = "今日新增分类数")
    private Long newCategoriesOfToday;
    
    @ApiModelProperty(value = "本月新增分类数")
    private Long newCategoriesOfMonth;
    
    @ApiModelProperty(value = "顶级分类数量")
    private Long topLevelCategories;
    
    @ApiModelProperty(value = "最大分类层级")
    private Integer maxCategoryLevel;
    
    @ApiModelProperty(value = "分类总增长率(与上月相比)")
    private Double categoryGrowthRate;
    
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
}