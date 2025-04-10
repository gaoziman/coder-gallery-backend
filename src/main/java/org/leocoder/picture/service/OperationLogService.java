package org.leocoder.picture.service;

import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.domain.dto.log.OperationLogQueryRequest;
import org.leocoder.picture.domain.pojo.OperationLog;
import org.leocoder.picture.domain.vo.log.OperationLogStatisticsVO;
import org.leocoder.picture.domain.vo.log.OperationLogVO;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 15:20
 * @description : 操作日志服务接口
 */
public interface OperationLogService {

    /**
     * 分页查询操作日志
     *
     * @param queryRequest 查询参数
     * @return 分页结果
     */
    PageResult<OperationLogVO> listOperationLogs(OperationLogQueryRequest queryRequest);

    /**
     * 根据ID获取操作日志详情
     *
     * @param id 日志ID
     * @return 日志详情
     */
    OperationLogVO getOperationLogById(Long id);

    /**
     * 删除操作日志
     *
     * @param id 日志ID
     * @return 是否成功
     */
    boolean deleteOperationLog(Long id);

    /**
     * 批量删除操作日志
     *
     * @param ids 日志ID列表
     * @return 是否成功
     */
    boolean batchDeleteOperationLogs(List<Long> ids);

    /**
     * 清空操作日志
     *
     * @return 是否成功
     */
    boolean clearOperationLogs();

    /**
     * 导出操作日志
     *
     * @param queryRequest 查询参数
     * @return 导出文件URL
     */
    String exportOperationLogs(OperationLogQueryRequest queryRequest);

    /**
     * 保存操作日志
     *
     * @param log 操作日志对象
     * @return 是否成功
     */
    boolean saveOperationLog(OperationLog log);

    /**
     * 异步保存操作日志
     *
     * @param log 操作日志对象
     */
    void asyncSaveOperationLog(OperationLog log);

    /**
     * 获取操作日志统计信息
     *
     * @return 操作日志统计信息
     */
    OperationLogStatisticsVO getOperationLogStatistics();
}