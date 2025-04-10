package org.leocoder.picture.domain.vo.log;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 登录统计VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "LoginStatisticsVO", description = "登录日志统计VO")
public class LoginStatisticsVO {

    @ApiModelProperty("总登录次数")
    private Long totalLoginCount;

    @ApiModelProperty("活跃用户数")
    private Long activeUserCount;

    @ApiModelProperty("登录成功率")
    private Double successRate;

    @ApiModelProperty("异常登录数")
    private Long abnormalLoginCount;

    @ApiModelProperty("今日登录次数")
    private Long todayLoginCount;

    @ApiModelProperty("今日登录用户数")
    private Long todayLoginUsers;

    @ApiModelProperty("本周登录次数")
    private Long weekLoginCount;

    @ApiModelProperty("本月登录次数")
    private Long monthLoginCount;

    @ApiModelProperty("今日登录失败次数")
    private Long todayFailCount;

    @ApiModelProperty("环比增长率（%）")
    private Double growthRate;
}