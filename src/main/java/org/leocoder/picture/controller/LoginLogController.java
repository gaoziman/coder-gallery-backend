package org.leocoder.picture.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.leocoder.picture.annotation.Log;
import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.domain.dto.log.LoginLogQueryRequest;
import org.leocoder.picture.domain.vo.log.LoginLogVO;
import org.leocoder.picture.domain.vo.log.LoginStatisticsVO;
import org.leocoder.picture.service.LoginLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 15:15
 * @description : 登录日志管理控制器
 */
@RestController
@RequestMapping("/admin/log/login")
@RequiredArgsConstructor
@Api(tags = "登录日志管理")
public class LoginLogController {

    private final LoginLogService loginLogService;

    @ApiOperation("分页查询登录日志")
    @GetMapping("/list")
    public Result<PageResult<LoginLogVO>> listLoginLogs(LoginLogQueryRequest queryRequest) {
        PageResult<LoginLogVO> pageResult = loginLogService.listLoginLogs(queryRequest);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("获取登录日志详情")
    @GetMapping("/{id}")
    public Result<LoginLogVO> getLoginLogDetail(@PathVariable Long id) {
        LoginLogVO logVO = loginLogService.getLoginLogById(id);
        return ResultUtils.success(logVO);
    }

    @ApiOperation("删除登录日志")
    @DeleteMapping("/{id}")
    @Log(module = "登录日志管理", action = "删除登录日志")
    public Result<Boolean> deleteLoginLog( @PathVariable Long id) {
        boolean result = loginLogService.deleteLoginLog(id);
        return ResultUtils.success(result);
    }

    @ApiOperation("批量删除登录日志")
    @DeleteMapping("/batch")
    @Log(module = "登录日志管理", action = "批量删除登录日志")
    public Result<Boolean> batchDeleteLoginLogs(@RequestBody List<Long> ids) {
        boolean result = loginLogService.batchDeleteLoginLogs(ids);
        return ResultUtils.success(result);
    }

    @ApiOperation("清空登录日志")
    @DeleteMapping("/clear")
    @Log(module = "登录日志管理", action = "清空登录日志")
    public Result<Boolean> clearLoginLogs() {
        boolean result = loginLogService.clearLoginLogs();
        return ResultUtils.success(result);
    }

    @ApiOperation("导出登录日志")
    @GetMapping("/export")
    @Log(module = "登录日志管理", action = "导出登录日志")
    public Result<String> exportLoginLogs(LoginLogQueryRequest queryRequest) {
        String fileUrl = loginLogService.exportLoginLogs(queryRequest);
        return ResultUtils.success(fileUrl);
    }

    @ApiOperation("获取登录统计信息")
    @GetMapping("/statistics")
    public Result<LoginStatisticsVO> getLoginStatistics() {
        LoginStatisticsVO statisticsVO = loginLogService.getLoginStatistics();
        return ResultUtils.success(statisticsVO);
    }
}