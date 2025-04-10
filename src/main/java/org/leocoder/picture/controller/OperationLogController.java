package org.leocoder.picture.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.leocoder.picture.annotation.Log;
import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.domain.dto.log.OperationLogQueryRequest;
import org.leocoder.picture.domain.vo.log.OperationLogStatisticsVO;
import org.leocoder.picture.domain.vo.log.OperationLogVO;
import org.leocoder.picture.service.OperationLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 15:10
 * @description : 操作日志管理控制器
 */
@RestController
@RequestMapping("/admin/log/operation")
@RequiredArgsConstructor
@Api(tags = "操作日志管理")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @ApiOperation("分页查询操作日志")
    @GetMapping("/list")
    public Result<PageResult<OperationLogVO>> listOperationLogs(OperationLogQueryRequest queryRequest) {
        PageResult<OperationLogVO> pageResult = operationLogService.listOperationLogs(queryRequest);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("获取操作日志详情")
    @GetMapping("/{id}")
    public Result<OperationLogVO> getOperationLogDetail( @PathVariable Long id) {
        OperationLogVO logVO = operationLogService.getOperationLogById(id);
        return ResultUtils.success(logVO);
    }

    @ApiOperation("删除操作日志")
    @DeleteMapping("/{id}")
    @Log(module = "操作日志管理", action = "删除操作日志")
    public Result<Boolean> deleteOperationLog(@PathVariable Long id) {
        boolean result = operationLogService.deleteOperationLog(id);
        return ResultUtils.success(result);
    }

    @ApiOperation("批量删除操作日志")
    @DeleteMapping("/batch")
    @Log(module = "操作日志管理", action = "批量删除操作日志")
    public Result<Boolean> batchDeleteOperationLogs(@RequestBody List<Long> ids) {
        boolean result = operationLogService.batchDeleteOperationLogs(ids);
        return ResultUtils.success(result);
    }

    @ApiOperation("清空操作日志")
    @DeleteMapping("/clear")
    @Log(module = "操作日志管理", action = "清空操作日志")
    public Result<Boolean> clearOperationLogs() {
        boolean result = operationLogService.clearOperationLogs();
        return ResultUtils.success(result);
    }

    @ApiOperation("导出操作日志")
    @GetMapping("/export")
    @Log(module = "操作日志管理", action = "导出操作日志")
    public Result<String> exportOperationLogs(OperationLogQueryRequest queryRequest) {
        String fileUrl = operationLogService.exportOperationLogs(queryRequest);
        return ResultUtils.success(fileUrl);
    }

    @ApiOperation("获取操作日志统计信息")
    @GetMapping("/statistics")
    public Result<OperationLogStatisticsVO> getOperationLogStatistics() {
        OperationLogStatisticsVO statisticsVO = operationLogService.getOperationLogStatistics();
        return ResultUtils.success(statisticsVO);
    }
}