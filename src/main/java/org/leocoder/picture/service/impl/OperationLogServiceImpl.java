package org.leocoder.picture.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.domain.dto.log.OperationLogQueryRequest;
import org.leocoder.picture.domain.pojo.OperationLog;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.log.OperationLogStatisticsVO;
import org.leocoder.picture.domain.vo.log.OperationLogVO;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.mapper.OperationLogMapper;
import org.leocoder.picture.service.OperationLogService;
import org.leocoder.picture.service.UserService;
import org.leocoder.picture.utils.SecurityUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 15:30
 * @description : 操作日志服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    private final UserService userService;


    /**
     * 分页查询操作日志
     *
     * @param queryRequest 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<OperationLogVO> listOperationLogs(OperationLogQueryRequest queryRequest) {
        // 参数检查
        if (ObjectUtil.isNull(queryRequest)) {
            queryRequest = new OperationLogQueryRequest();
        }

        Integer pageNum = queryRequest.getPageNum();
        Integer pageSize = queryRequest.getPageSize();

        // 设置默认值
        if (ObjectUtil.isNull(pageNum) || pageNum < 1) {
            pageNum = 1;
        }
        if (ObjectUtil.isNull(pageSize) || pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        // 计算分页起始位置
        int offset = (pageNum - 1) * pageSize;

        // 查询总数
        Long total = operationLogMapper.countOperationLogs(queryRequest);

        // 如果没有数据，直接返回空结果
        if (total == 0) {
            return PageResult.build(0L, Collections.emptyList(), pageNum, pageSize);
        }

        // 查询分页数据
        List<OperationLog> logList = operationLogMapper.listOperationLogsByPage(queryRequest, offset, pageSize);

        // 转换为VO对象
        List<OperationLogVO> logVOList = logList.stream().map(log -> {
            OperationLogVO vo = new OperationLogVO();
            BeanUtil.copyProperties(log, vo);

            // 获取用户信息
            if (log.getUserId() != null) {
                User user = userService.getUsernameById(log.getUserId());
                vo.setUsername(user.getUsername());
                vo.setAvatar(user.getAvatar());
                vo.setRole(user.getRole());
            }

            return vo;
        }).collect(Collectors.toList());

        // 封装并返回分页结果
        return PageResult.build(total, logVOList, pageNum, pageSize);
    }


    /**
     * 根据ID获取操作日志详情
     *
     * @param id 日志ID
     * @return 日志详情
     */
    @Override
    public OperationLogVO getOperationLogById(Long id) {
        if (ObjectUtil.isNull(id) || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日志ID不合法");
        }

        // 查询日志
        OperationLog log = operationLogMapper.selectById(id);
        if (ObjectUtil.isNull(log) || log.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "日志不存在");
        }

        // 转换为VO对象
        OperationLogVO vo = new OperationLogVO();
        BeanUtil.copyProperties(log, vo);

        // 获取用户信息
        if (log.getUserId() != null) {
            User user = userService.getUsernameById(log.getUserId());
            vo.setUsername(user.getUsername());
        }

        return vo;
    }


    /**
     * 删除操作日志
     *
     * @param id 日志ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOperationLog(Long id) {
        if (ObjectUtil.isNull(id) || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日志ID不合法");
        }

        // 查询日志
        OperationLog log = operationLogMapper.selectById(id);
        if (ObjectUtil.isNull(log) || log.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "日志不存在");
        }

        // 逻辑删除
        OperationLog updateLog = new OperationLog();
        updateLog.setId(id);
        updateLog.setIsDeleted(1);
        updateLog.setUpdateTime(LocalDateTime.now());
        updateLog.setUpdateBy(SecurityUtils.getCurrentUserId());

        int result = operationLogMapper.updateByPrimaryKeySelective(updateLog);
        return result > 0;
    }


    /**
     * 批量删除操作日志
     *
     * @param ids 日志ID列表
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteOperationLogs(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日志ID列表不能为空");
        }

        // 批量逻辑删除
        int result = operationLogMapper.batchLogicDelete(ids, LocalDateTime.now(), SecurityUtils.getCurrentUserId());
        return result > 0;
    }


    /**
     * 清空操作日志
     *
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clearOperationLogs() {
        // 清空所有操作日志（逻辑删除）
        int result = operationLogMapper.clearAllLogs(LocalDateTime.now(), SecurityUtils.getCurrentUserId());
        return result > 0;
    }


    /**
     * 导出操作日志
     *
     * @param queryRequest 查询参数
     * @return 导出文件URL
     */
    @Override
    public String exportOperationLogs(OperationLogQueryRequest queryRequest) {
        // 查询数据
        List<OperationLog> logList;
        if (ObjectUtil.isNull(queryRequest) || Boolean.TRUE.equals(queryRequest.getExportAll())) {
            // 导出全部（不分页）
            logList = operationLogMapper.listAllOperationLogs(queryRequest);
        } else {
            // 根据条件查询（不分页）
            logList = operationLogMapper.listOperationLogsByPage(queryRequest, null, null);
        }

        if (CollUtil.isEmpty(logList)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "没有符合条件的日志数据");
        }

        // 转换为Excel导出对象
        List<OperationLogVO> exportList = new ArrayList<>(logList.size());
        for (OperationLog log : logList) {
            OperationLogVO vo = new OperationLogVO();
            BeanUtil.copyProperties(log, vo);

            // 获取用户信息
            if (log.getUserId() != null) {
                User user = userService.getUsernameById(log.getUserId());
                vo.setUsername(user.getUsername());
            }

            exportList.add(vo);
        }

        // 生成Excel文件
        String fileName = "操作日志_" + DateUtil.format(new java.util.Date(), "yyyyMMddHHmmss") + ".xlsx";
        String filePath = System.getProperty("java.io.tmpdir") + File.separator + fileName;

        // 使用Hutool的ExcelWriter
        ExcelWriter writer = ExcelUtil.getWriter(filePath);

        // 设置列宽
        writer.setColumnWidth(0, 20);
        writer.setColumnWidth(1, 20);
        writer.setColumnWidth(2, 15);
        writer.setColumnWidth(3, 15);
        writer.setColumnWidth(4, 30);
        writer.setColumnWidth(5, 15);
        writer.setColumnWidth(6, 15);
        writer.setColumnWidth(7, 20);

        // 设置表头
        writer.addHeaderAlias("id", "日志ID");
        writer.addHeaderAlias("userId", "用户ID");
        writer.addHeaderAlias("username", "用户名");
        writer.addHeaderAlias("module", "操作模块");
        writer.addHeaderAlias("action", "操作类型");
        writer.addHeaderAlias("method", "请求方法");
        writer.addHeaderAlias("params", "请求参数");
        writer.addHeaderAlias("time", "执行时长(ms)");
        writer.addHeaderAlias("ip", "操作IP");
        writer.addHeaderAlias("operationTime", "操作时间");
        writer.addHeaderAlias("status", "操作状态");
        writer.addHeaderAlias("errorMsg", "错误消息");

        // 写入数据
        writer.write(exportList, true);
        writer.close();

        // 返回文件URL（实际项目中可能需要上传到云存储）
        return filePath;
    }


    /**
     * 保存操作日志
     *
     * @param log 操作日志对象
     * @return 是否成功
     */
    @Override
    public boolean saveOperationLog(OperationLog log) {
        if (ObjectUtil.isNull(log)) {
            return false;
        }

        // 设置默认值
        if (log.getCreateTime() == null) {
            log.setCreateTime(LocalDateTime.now());
        }
        if (log.getIsDeleted() == null) {
            log.setIsDeleted(0);
        }

        int result = operationLogMapper.insert(log);
        return result > 0;
    }


    /**
     * 异步保存操作日志
     *
     * @param log 操作日志对象
     */
    @Override
    @Async
    public void asyncSaveOperationLog(OperationLog log) {
        try {
            saveOperationLog(log);
        } catch (Exception e) {
            OperationLogServiceImpl.log.error("异步保存操作日志失败", e);
        }
    }

    /**
     * 获取操作日志统计信息
     *
     * @return 操作日志统计信息
     */
    @Override
    public OperationLogStatisticsVO getOperationLogStatistics() {
        // 获取总操作次数
        Long totalOperations = operationLogMapper.countTotalOperations();
        Long totalOperationsLastYear = operationLogMapper.countTotalOperationsLastYear();

        // 计算同比增长率
        Double totalOperationsGrowthRate = calculateGrowthRate(totalOperations, totalOperationsLastYear);

        // 获取今日操作量
        Long todayOperations = operationLogMapper.countTodayOperations();
        Long yesterdayOperations = operationLogMapper.countYesterdayOperations();

        // 计算今日操作量环比增长率
        Double todayOperationsGrowthRate = calculateGrowthRate(todayOperations, yesterdayOperations);

        // 获取本月操作量
        Long monthlyOperations = operationLogMapper.countMonthlyOperations();
        Long lastMonthOperations = operationLogMapper.countLastMonthOperations();

        // 计算环比增长率
        Double monthlyOperationsGrowthRate = calculateGrowthRate(monthlyOperations, lastMonthOperations);

        // 获取成功操作数量和总操作数
        Long successOperations = operationLogMapper.countSuccessOperations();
        Long allOperations = operationLogMapper.countAllOperationsForSuccessRate();

        // 计算成功率
        Double successRate = calculateRate(successOperations, allOperations);

        // 获取上月成功操作数量和上月总操作数
        Long lastMonthSuccessOperations = operationLogMapper.countLastMonthSuccessOperations();
        Long lastMonthAllOperations = operationLogMapper.countLastMonthAllOperationsForSuccessRate();

        // 计算上月成功率
        Double lastMonthSuccessRate = calculateRate(lastMonthSuccessOperations, lastMonthAllOperations);

        // 计算成功率环比增长率
        Double successRateGrowthRate = calculateGrowthRate(successRate, lastMonthSuccessRate);

        // 获取异常操作数量
        Long exceptionOperations = operationLogMapper.countExceptionOperations();
        Long lastMonthExceptionOperations = operationLogMapper.countLastMonthExceptionOperations();

        // 计算异常操作环比增长率
        Double exceptionOperationsGrowthRate = calculateGrowthRate(exceptionOperations, lastMonthExceptionOperations);

        // 获取活跃用户数
        Long activeUsers = operationLogMapper.countActiveUsers();
        Long lastMonthActiveUsers = operationLogMapper.countLastMonthActiveUsers();

        // 计算活跃用户数环比增长率
        Double activeUsersGrowthRate = calculateGrowthRate(activeUsers, lastMonthActiveUsers);

        // 构建并返回统计VO对象
        return OperationLogStatisticsVO.builder()
                .totalOperations(totalOperations)
                .totalOperationsGrowthRate(totalOperationsGrowthRate)
                .todayOperations(todayOperations)
                .todayOperationsGrowthRate(todayOperationsGrowthRate)
                .monthlyOperations(monthlyOperations)
                .monthlyOperationsGrowthRate(monthlyOperationsGrowthRate)
                .successRate(successRate)
                .successRateGrowthRate(successRateGrowthRate)
                .exceptionOperations(exceptionOperations)
                .exceptionOperationsGrowthRate(exceptionOperationsGrowthRate)
                .activeUsers(activeUsers)
                .activeUsersGrowthRate(activeUsersGrowthRate)
                .build();
    }
    /**
     * 计算增长率
     *
     * @param current 当前值
     * @param previous 前一个值
     * @return 增长率（百分比）
     */
    private Double calculateGrowthRate(Long current, Long previous) {
        if (previous == null || previous == 0) {
            return 0.0;
        }

        // 转换为 BigDecimal 进行精确计算
        BigDecimal currentBD = new BigDecimal(current);
        BigDecimal previousBD = new BigDecimal(previous);
        BigDecimal hundred = new BigDecimal(100);

        // (current - previous) / previous * 100
        return currentBD.subtract(previousBD)
                .divide(previousBD, 4, RoundingMode.HALF_UP)
                .multiply(hundred)
                .doubleValue();
    }

    /**
     * 计算增长率（Double类型）
     *
     * @param current 当前值
     * @param previous 前一个值
     * @return 增长率（百分比）
     */
    private Double calculateGrowthRate(Double current, Double previous) {
        if (previous == null || previous == 0) {
            return 0.0;
        }

        // 转换为 BigDecimal 进行精确计算
        BigDecimal currentBD = BigDecimal.valueOf(current);
        BigDecimal previousBD = BigDecimal.valueOf(previous);
        BigDecimal hundred = new BigDecimal(100);

        // (current - previous) / previous * 100
        return currentBD.subtract(previousBD)
                .divide(previousBD, 4, RoundingMode.HALF_UP)
                .multiply(hundred)
                .doubleValue();
    }

    /**
     * 计算比率
     *
     * @param numerator 分子
     * @param denominator 分母
     * @return 比率（百分比）
     */
    private Double calculateRate(Long numerator, Long denominator) {
        if (denominator == null || denominator == 0) {
            return 0.0;
        }

        // 转换为 BigDecimal 进行精确计算
        BigDecimal numeratorBD = new BigDecimal(numerator);
        BigDecimal denominatorBD = new BigDecimal(denominator);
        BigDecimal hundred = new BigDecimal(100);

        // numerator / denominator * 100
        return numeratorBD.divide(denominatorBD, 4, RoundingMode.HALF_UP)
                .multiply(hundred)
                .doubleValue();
    }
}