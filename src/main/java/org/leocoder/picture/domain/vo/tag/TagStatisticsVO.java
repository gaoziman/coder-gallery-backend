package org.leocoder.picture.domain.vo.tag;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 10:51
 * @description : 标签统计信息视图对象
 */
@Data
@ApiModel(description = "标签统计信息视图对象")
public class TagStatisticsVO {

    @ApiModelProperty(value = "标签总数", example = "100")
    private Integer totalCount;

    @ApiModelProperty(value = "已启用标签数", example = "80")
    private Integer activeCount;

    @ApiModelProperty(value = "未启用标签数", example = "20")
    private Integer inactiveCount;

    @ApiModelProperty(value = "今日创建标签数", example = "5")
    private Integer todayCount;

    @ApiModelProperty(value = "本月新增标签数", example = "15")
    private Integer monthCount;

    @ApiModelProperty(value = "本周新增标签数", example = "15")
    private Integer weekCount;

    @ApiModelProperty(value = "未使用的标签数", example = "10")
    private  Integer unusedTag;

    @ApiModelProperty(value = "引用总数", example = "200")
    private Integer totalReferenceCount;
}