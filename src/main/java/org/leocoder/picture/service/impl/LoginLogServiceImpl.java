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
import org.leocoder.picture.domain.dto.log.LoginLogQueryRequest;
import org.leocoder.picture.domain.pojo.LoginLog;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.log.LoginLogVO;
import org.leocoder.picture.domain.vo.log.LoginStatisticsVO;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.mapper.LoginLogMapper;
import org.leocoder.picture.service.LoginLogService;
import org.leocoder.picture.service.UserService;
import org.leocoder.picture.utils.SecurityUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 15:35
 * @description : 登录日志服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogMapper loginLogMapper;

    private final UserService userService;


    /**
     * 分页查询登录日志
     *
     * @param queryRequest 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<LoginLogVO> listLoginLogs(LoginLogQueryRequest queryRequest) {
        // 参数检查
        if (ObjectUtil.isNull(queryRequest)) {
            queryRequest = new LoginLogQueryRequest();
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
        Long total = loginLogMapper.countLoginLogs(queryRequest);

        // 如果没有数据，直接返回空结果
        if (total == 0) {
            return PageResult.build(0L, Collections.emptyList(), pageNum, pageSize);
        }

        // 查询分页数据
        List<LoginLog> logList = loginLogMapper.listLoginLogsByPage(queryRequest, offset, pageSize);

        // 转换为VO对象
        List<LoginLogVO> logVOList = logList.stream().map(log -> {
            LoginLogVO vo = new LoginLogVO();
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
     * 根据ID获取登录日志详情
     *
     * @param id 日志ID
     * @return 日志详情
     */
    @Override
    public LoginLogVO getLoginLogById(Long id) {
        if (ObjectUtil.isNull(id) || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日志ID不合法");
        }

        // 查询日志
        LoginLog log = loginLogMapper.selectById(id);
        if (ObjectUtil.isNull(log) || log.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "日志不存在");
        }

        // 转换为VO对象
        LoginLogVO vo = new LoginLogVO();
        BeanUtil.copyProperties(log, vo);

        // 获取用户信息
        if (log.getUserId() != null) {
            User user = userService.getUsernameById(log.getUserId());
            vo.setUsername(user.getUsername());
        }

        return vo;
    }


    /**
     * 删除登录日志
     *
     * @param id 日志ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLoginLog(Long id) {
        if (ObjectUtil.isNull(id) || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日志ID不合法");
        }

        // 查询日志
        LoginLog log = loginLogMapper.selectById(id);
        if (ObjectUtil.isNull(log) || log.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "日志不存在");
        }

        // 逻辑删除
        LoginLog updateLog = new LoginLog();
        updateLog.setId(id);
        updateLog.setIsDeleted(1);
        updateLog.setUpdateTime(LocalDateTime.now());
        updateLog.setUpdateBy(SecurityUtils.getCurrentUserId());

        int result = loginLogMapper.updateByPrimaryKeySelective(updateLog);
        return result > 0;
    }


    /**
     * 批量删除登录日志
     *
     * @param ids 日志ID列表
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteLoginLogs(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日志ID列表不能为空");
        }

        // 批量逻辑删除
        int result = loginLogMapper.batchLogicDelete(ids, LocalDateTime.now(), SecurityUtils.getCurrentUserId());
        return result > 0;
    }


    /**
     * 清空登录日志
     *
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clearLoginLogs() {
        // 清空所有登录日志（逻辑删除）
        int result = loginLogMapper.clearAllLogs(LocalDateTime.now(), SecurityUtils.getCurrentUserId());
        return result > 0;
    }


    /**
     * 导出登录日志
     *
     * @param queryRequest 查询参数
     * @return 导出文件URL
     */
    @Override
    public String exportLoginLogs(LoginLogQueryRequest queryRequest) {
        // 查询数据
        List<LoginLog> logList;
        if (ObjectUtil.isNull(queryRequest) || Boolean.TRUE.equals(queryRequest.getExportAll())) {
            // 导出全部（不分页）
            logList = loginLogMapper.listAllLoginLogs(queryRequest);
        } else {
            // 根据条件查询（不分页）
            logList = loginLogMapper.listLoginLogsByPage(queryRequest, null, null);
        }

        if (CollUtil.isEmpty(logList)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "没有符合条件的日志数据");
        }

        // 转换为Excel导出对象
        List<LoginLogVO> exportList = new ArrayList<>(logList.size());
        for (LoginLog log : logList) {
            LoginLogVO vo = new LoginLogVO();
            BeanUtil.copyProperties(log, vo);

            // 获取用户信息
            if (log.getUserId() != null) {
                User user = userService.getUsernameById(log.getUserId());
                vo.setUsername(user.getUsername());
            }

            exportList.add(vo);
        }

        // 生成Excel文件
        String fileName = "登录日志_" + DateUtil.format(new java.util.Date(), "yyyyMMddHHmmss") + ".xlsx";
        String filePath = System.getProperty("java.io.tmpdir") + File.separator + fileName;

        // 使用Hutool的ExcelWriter
        ExcelWriter writer = ExcelUtil.getWriter(filePath);

        // 设置列宽
        writer.setColumnWidth(0, 20);
        writer.setColumnWidth(1, 20);
        writer.setColumnWidth(2, 15);
        writer.setColumnWidth(3, 20);
        writer.setColumnWidth(4, 20);
        writer.setColumnWidth(5, 20);
        writer.setColumnWidth(6, 20);
        writer.setColumnWidth(7, 15);
        writer.setColumnWidth(8, 15);

        // 设置表头
        writer.addHeaderAlias("id", "日志ID");
        writer.addHeaderAlias("userId", "用户ID");
        writer.addHeaderAlias("username", "用户名");
        writer.addHeaderAlias("loginTime", "登录时间");
        writer.addHeaderAlias("logoutTime", "登出时间");
        writer.addHeaderAlias("ip", "登录IP");
        writer.addHeaderAlias("location", "登录地点");
        writer.addHeaderAlias("device", "登录设备");
        writer.addHeaderAlias("browser", "浏览器");
        writer.addHeaderAlias("os", "操作系统");
        writer.addHeaderAlias("status", "登录状态");
        writer.addHeaderAlias("message", "登录消息");

        // 写入数据
        writer.write(exportList, true);
        writer.close();

        // 返回文件URL（实际项目中可能需要上传到云存储）
        return filePath;
    }


    /**
     * 保存登录日志
     *
     * @param log 登录日志对象
     * @return 是否成功
     */
    @Override
    public boolean saveLoginLog(LoginLog log) {
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

        int result = loginLogMapper.insert(log);
        return result > 0;
    }


    /**
     * 异步保存登录日志
     *
     * @param log 登录日志对象
     */
    @Override
    @Async
    public void asyncSaveLoginLog(LoginLog log) {
        try {
            saveLoginLog(log);
        } catch (Exception e) {
            LoginLogServiceImpl.log.error("异步保存登录日志失败", e);
        }
    }


    /**
     * 获取登录统计信息
     *
     * @return 统计信息
     */
    @Override
    public LoginStatisticsVO getLoginStatistics() {
        // 创建统计VO对象
        LoginStatisticsVO statisticsVO = new LoginStatisticsVO();

        // 总登录次数
        Long totalLoginCount = loginLogMapper.countTotalLogins();
        statisticsVO.setTotalLoginCount(totalLoginCount);

        // 活跃用户数
        Long activeUserCount = loginLogMapper.countActiveUsers();
        statisticsVO.setActiveUserCount(activeUserCount);

        // 登录成功率
        Double successRate = loginLogMapper.calculateSuccessRate();
        statisticsVO.setSuccessRate(successRate);

        // 异常登录数
        Long abnormalLoginCount = loginLogMapper.countAbnormalLogins();
        statisticsVO.setAbnormalLoginCount(abnormalLoginCount);

        // 今日登录次数
        Long todayLoginCount = loginLogMapper.countTodayLogins();
        statisticsVO.setTodayLoginCount(todayLoginCount);

        // 今日登录用户数
        Long todayLoginUsers = loginLogMapper.countTodayLoginUsers();
        statisticsVO.setTodayLoginUsers(todayLoginUsers);

        // 本周登录次数
        Long weekLoginCount = loginLogMapper.countWeekLogins();
        statisticsVO.setWeekLoginCount(weekLoginCount);

        // 本月登录次数
        Long monthLoginCount = loginLogMapper.countMonthLogins();
        statisticsVO.setMonthLoginCount(monthLoginCount);

        // 登录失败次数（今日）
        Long todayFailCount = loginLogMapper.countTodayFailLogins();
        statisticsVO.setTodayFailCount(todayFailCount);

        // 计算环比增长率
        Long yesterdayLoginCount = loginLogMapper.countYesterdayLogins();
        if (yesterdayLoginCount != null && yesterdayLoginCount > 0) {
            Double growthRate = ((todayLoginCount - yesterdayLoginCount) * 100.0) / yesterdayLoginCount;
            statisticsVO.setGrowthRate(growthRate);
        } else {
            statisticsVO.setGrowthRate(0.0);
        }

        return statisticsVO;
    }
}