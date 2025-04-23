package org.leocoder.picture.domain.vo.log;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 操作日志统计VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "OperationLogStatisticsVO", description = "操作日志统计VO")
public class OperationLogStatisticsVO implements Serializable {

    private static final long serialVersionUID = 3366098390405610515L;

    @ApiModelProperty(value = "总操作次数")
    private Long totalOperations;

    @ApiModelProperty(value = "总操作次数同比增长率")
    private Double totalOperationsGrowthRate;

    @ApiModelProperty(value = "今日操作量")
    private Long todayOperations;

    @ApiModelProperty(value = "今日操作量环比增长率")
    private Double todayOperationsGrowthRate;

    @ApiModelProperty(value = "本月操作量")
    private Long monthlyOperations;

    @ApiModelProperty(value = "本月操作量环比增长率")
    private Double monthlyOperationsGrowthRate;

    @ApiModelProperty(value = "操作成功率")
    private Double successRate;

    @ApiModelProperty(value = "操作成功率环比增长率")
    private Double successRateGrowthRate;

    @ApiModelProperty(value = "异常操作数")
    private Long exceptionOperations;

    @ApiModelProperty(value = "异常操作环比增长率")
    private Double exceptionOperationsGrowthRate;

    @ApiModelProperty(value = "活跃用户数")
    private Long activeUsers;

    @ApiModelProperty(value = "活跃用户数环比增长率")
    private Double activeUsersGrowthRate;
}