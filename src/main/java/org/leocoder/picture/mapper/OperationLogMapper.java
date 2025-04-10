package org.leocoder.picture.mapper;

import org.apache.ibatis.annotations.Param;
import org.leocoder.picture.domain.dto.log.OperationLogQueryRequest;
import org.leocoder.picture.domain.pojo.OperationLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 13:26
 * @description : 用户操作日志Mapper接口
 */
public interface OperationLogMapper {

    OperationLog selectById(Long id);

    int deleteById(Long id);

    int insert(OperationLog record);

    int insertSelective(OperationLog record);

    int updateByPrimaryKeySelective(OperationLog record);

    int updateById(OperationLog record);


    /**
     * 通过制定id插入操作日志
     *
     * @param record 操作日志对象
     * @return 影响行数
     */
    int insertWithId(OperationLog record);

    /**
     * 根据条件查询操作日志总数
     *
     * @param queryRequest 查询条件
     * @return 日志数量
     */
    Long countOperationLogs(@Param("queryRequest") OperationLogQueryRequest queryRequest);

    /**
     * 分页查询操作日志
     *
     * @param queryRequest 查询条件
     * @param offset       偏移量
     * @param pageSize     页大小
     * @return 日志列表
     */
    List<OperationLog> listOperationLogsByPage(
            @Param("queryRequest") OperationLogQueryRequest queryRequest,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 查询所有操作日志（导出用）
     *
     * @param queryRequest 查询条件
     * @return 日志列表
     */
    List<OperationLog> listAllOperationLogs(@Param("queryRequest") OperationLogQueryRequest queryRequest);

    /**
     * 批量逻辑删除操作日志
     *
     * @param ids        日志ID列表
     * @param updateTime 更新时间
     * @param updateBy   更新人
     * @return 影响行数
     */
    int batchLogicDelete(
            @Param("ids") List<Long> ids,
            @Param("updateTime") LocalDateTime updateTime,
            @Param("updateBy") Long updateBy);

    /**
     * 清空所有操作日志（逻辑删除）
     *
     * @param updateTime 更新时间
     * @param updateBy   更新人
     * @return 影响行数
     */
    int clearAllLogs(
            @Param("updateTime") LocalDateTime updateTime,
            @Param("updateBy") Long updateBy);

    /**
     * 获取总操作次数
     *
     * @return 总操作次数
     */
    Long countTotalOperations();

    /**
     * 获取上一年同期的总操作次数（用于同比计算）
     *
     * @return 上一年同期的总操作次数
     */
    Long countTotalOperationsLastYear();

    /**
     * 获取本月操作量
     *
     * @return 本月操作量
     */
    Long countMonthlyOperations();

    /**
     * 获取上月操作量（用于环比计算）
     *
     * @return 上月操作量
     */
    Long countLastMonthOperations();

    /**
     * 获取成功操作数量
     *
     * @return 成功操作数量
     */
    Long countSuccessOperations();

    /**
     * 获取上月成功操作数量（用于环比计算）
     *
     * @return 上月成功操作数量
     */
    Long countLastMonthSuccessOperations();

    /**
     * 获取总操作数（用于计算成功率）
     *
     * @return 总操作数
     */
    Long countAllOperationsForSuccessRate();

    /**
     * 获取上月总操作数（用于计算上月成功率）
     *
     * @return 上月总操作数
     */
    Long countLastMonthAllOperationsForSuccessRate();

    /**
     * 获取异常操作数量
     *
     * @return 异常操作数量
     */
    Long countExceptionOperations();

    /**
     * 获取上月异常操作数量（用于环比计算）
     *
     * @return 上月异常操作数量
     */
    Long countLastMonthExceptionOperations();

    /**
     * 获取今日操作量
     *
     * @return 今日操作量
     */
    Long countTodayOperations();

    /**
     * 获取昨日操作量（用于环比计算）
     *
     * @return 昨日操作量
     */
    Long countYesterdayOperations();

    /**
     * 获取本月活跃用户数
     *
     * @return 活跃用户数
     */
    Long countActiveUsers();

    /**
     * 获取上月活跃用户数（用于环比计算）
     *
     * @return 上月活跃用户数
     */
    Long countLastMonthActiveUsers();
}