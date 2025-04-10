package org.leocoder.picture.service;

import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.domain.dto.log.LoginLogQueryRequest;
import org.leocoder.picture.domain.pojo.LoginLog;
import org.leocoder.picture.domain.vo.log.LoginLogVO;
import org.leocoder.picture.domain.vo.log.LoginStatisticsVO;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 15:25
 * @description : 登录日志服务接口
 */
public interface LoginLogService {

    /**
     * 分页查询登录日志
     *
     * @param queryRequest 查询参数
     * @return 分页结果
     */
    PageResult<LoginLogVO> listLoginLogs(LoginLogQueryRequest queryRequest);

    /**
     * 根据ID获取登录日志详情
     *
     * @param id 日志ID
     * @return 日志详情
     */
    LoginLogVO getLoginLogById(Long id);

    /**
     * 删除登录日志
     *
     * @param id 日志ID
     * @return 是否成功
     */
    boolean deleteLoginLog(Long id);

    /**
     * 批量删除登录日志
     *
     * @param ids 日志ID列表
     * @return 是否成功
     */
    boolean batchDeleteLoginLogs(List<Long> ids);

    /**
     * 清空登录日志
     *
     * @return 是否成功
     */
    boolean clearLoginLogs();

    /**
     * 导出登录日志
     *
     * @param queryRequest 查询参数
     * @return 导出文件URL
     */
    String exportLoginLogs(LoginLogQueryRequest queryRequest);

    /**
     * 保存登录日志
     *
     * @param log 登录日志对象
     * @return 是否成功
     */
    boolean saveLoginLog(LoginLog log);

    /**
     * 异步保存登录日志
     *
     * @param log 登录日志对象
     */
    void asyncSaveLoginLog(LoginLog log);

    /**
     * 获取登录统计信息
     *
     * @return 统计信息
     */
    LoginStatisticsVO getLoginStatistics();
}