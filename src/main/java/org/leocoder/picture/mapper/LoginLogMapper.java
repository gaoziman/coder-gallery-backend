package org.leocoder.picture.mapper;

import org.apache.ibatis.annotations.Param;
import org.leocoder.picture.domain.dto.log.LoginLogQueryRequest;
import org.leocoder.picture.domain.pojo.LoginLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : 程序员Leo
 * @date 2025-04-10
 * @description : 登录日志Mapper接口
 */
public interface LoginLogMapper {

    /**
     * 根据ID查询登录日志
     *
     * @param id 日志ID
     * @return 登录日志对象
     */
    LoginLog selectById(Long id);

    /**
     * 插入登录日志
     *
     * @param record 登录日志对象
     * @return 影响行数
     */
    int insert(LoginLog record);

    /**
     * 根据ID插入登录日志
     *
     * @param record 登录日志对象
     * @return 影响行数
     */
    int insertWithId(LoginLog record);


    /**
     * 选择性插入登录日志
     *
     * @param record 登录日志对象
     * @return 影响行数
     */
    int insertSelective(LoginLog record);

    /**
     * 根据ID选择性更新登录日志
     *
     * @param record 登录日志对象
     * @return 影响行数
     */
    int updateByPrimaryKeySelective(LoginLog record);

    /**
     * 根据条件统计登录日志数量
     *
     * @param queryRequest 查询条件
     * @return 日志数量
     */
    Long countLoginLogs(@Param("queryRequest") LoginLogQueryRequest queryRequest);

    /**
     * 分页查询登录日志
     *
     * @param queryRequest 查询条件
     * @param offset       起始位置
     * @param pageSize     每页大小
     * @return 日志列表
     */
    List<LoginLog> listLoginLogsByPage(@Param("queryRequest") LoginLogQueryRequest queryRequest,
                                       @Param("offset") Integer offset,
                                       @Param("pageSize") Integer pageSize);

    /**
     * 查询所有登录日志（导出用）
     *
     * @param queryRequest 查询条件
     * @return 日志列表
     */
    List<LoginLog> listAllLoginLogs(@Param("queryRequest") LoginLogQueryRequest queryRequest);

    /**
     * 批量逻辑删除登录日志
     *
     * @param ids        ID列表
     * @param updateTime 更新时间
     * @param updateBy   更新人
     * @return 影响行数
     */
    int batchLogicDelete(@Param("ids") List<Long> ids,
                         @Param("updateTime") LocalDateTime updateTime,
                         @Param("updateBy") Long updateBy);

    /**
     * 清空所有登录日志（逻辑删除）
     *
     * @param updateTime 更新时间
     * @param updateBy   更新人
     * @return 影响行数
     */
    int clearAllLogs(@Param("updateTime") LocalDateTime updateTime,
                     @Param("updateBy") Long updateBy);

    /**
     * 统计今日登录次数
     *
     * @return 登录次数
     */
    Long countTodayLogins();

    /**
     * 统计今日登录用户数
     *
     * @return 用户数
     */
    Long countTodayLoginUsers();

    /**
     * 统计本周登录次数
     *
     * @return 登录次数
     */
    Long countWeekLogins();

    /**
     * 统计本月登录次数
     *
     * @return 登录次数
     */
    Long countMonthLogins();

    /**
     * 统计今日登录失败次数
     *
     * @return 失败次数
     */
    Long countTodayFailLogins();

    /**
     * 统计总登录次数
     *
     * @return 总登录次数
     */
    Long countTotalLogins();

    /**
     * 统计活跃用户数（过去30天有登录记录的用户）
     *
     * @return 活跃用户数
     */
    Long countActiveUsers();

    /**
     * 统计登录成功率
     *
     * @return 成功率
     */
    Double calculateSuccessRate();

    /**
     * 统计异常登录次数（非常规时间或IP异常的登录）
     *
     * @return 异常登录次数
     */
    Long countAbnormalLogins();

    /**
     * 统计昨日登录次数（用于计算环比增长）
     *
     * @return 昨日登录次数
     */
    Long countYesterdayLogins();

    /**
     * 通过IP查找最近的登录记录
     *
     * @param ip IP地址
     * @return 登录记录
     */
    LoginLog findLatestLoginByIp(String ip);

    /**
     * 查找该用户最近的未登出记录，记录完整的会话
     *
     * @param userId 用户ID
     * @return 登录记录
     */
    LoginLog findLatestLoginWithoutLogout(Long userId);
}