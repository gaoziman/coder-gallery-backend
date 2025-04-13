package org.leocoder.picture.domain.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08
 * @description : 用户统计信息VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "用户统计信息VO")
public class UserStatisticsVO {
    @ApiModelProperty
    private Long totalUsers;

    @ApiModelProperty(value = "本月新增用户数")
    private Long newUsersThisMonth;

    @ApiModelProperty(value = "VIP用户数")
    private Long vipUsers;

    @ApiModelProperty(value = "活跃用户比例(%)")
    private Double activeUserRatio;

    @ApiModelProperty(value = "总用户增长率(%)，与上月相比")
    private Double totalUserGrowth;

    @ApiModelProperty(value = "新增用户增长率(%)，与上月相比")
    private Double newUserGrowth;

    @ApiModelProperty(value = "VIP用户增长率(%)，与上月相比")
    private Double vipUserGrowth;

    @ApiModelProperty(value = "活跃用户比例变化(百分点)，与上月相比")
    private Double activeGrowth;

    @ApiModelProperty(value = "今日登录用户数量")
    private Long todayLoginUsers;

    @ApiModelProperty(value = "本周新增用户数量")
    private Long newUsersThisWeek;

    @ApiModelProperty(value = "冻结账户数量")
    private Long bannedUsers;
}