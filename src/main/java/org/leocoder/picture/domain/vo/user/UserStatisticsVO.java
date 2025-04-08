package org.leocoder.picture.domain.vo.user;

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
public class UserStatisticsVO {
    /**
     * 总用户数
     */
    private Long totalUsers;

    /**
     * 本月新增用户数
     */
    private Long newUsersThisMonth;

    /**
     * VIP用户数
     */
    private Long vipUsers;

    /**
     * 活跃用户比例(%)
     */
    private Double activeUserRatio;

    /**
     * 总用户增长率(%)，与上月相比
     */
    private Double totalUserGrowth;

    /**
     * 新增用户增长率(%)，与上月相比
     */
    private Double newUserGrowth;

    /**
     * VIP用户增长率(%)，与上月相比
     */
    private Double vipUserGrowth;

    /**
     * 活跃用户比例变化(百分点)，与上月相比
     */
    private Double activeGrowth;

    /**
     * 今日登录用户数量
     */
    private Long todayLoginUsers;

    /**
     * 本周新增用户数量
     */
    private Long newUsersThisWeek;

    /**
     * 冻结账户数量
     */
    private Long bannedUsers;
}