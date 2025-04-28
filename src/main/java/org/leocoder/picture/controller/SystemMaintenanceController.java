package org.leocoder.picture.controller.admin;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.domain.dto.picture.DataRepairServiceImpl;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-27
 * @description : 系统维护管理接口
 */
@RestController
@RequestMapping("/admin/system")
@RequiredArgsConstructor
@Api(tags = "系统维护管理接口")
public class SystemMaintenanceController {

    private final DataRepairServiceImpl dataRepairService;

    @PostMapping("/repair/tag-counts")
    @ApiOperation("修复所有标签引用计数")
    public Result<Boolean> repairAllTagCounts() {
        // 触发立即执行修复任务
        dataRepairService.repairAllTagReferenceCount();
        return ResultUtils.success(true);
    }

    @PostMapping("/repair/tag-count/{tagId}")
    @ApiOperation("修复指定标签引用计数")
    public Result<Boolean> repairTagCount(@PathVariable Long tagId) {
        boolean result = dataRepairService.repairTagReferenceCount(tagId);
        return ResultUtils.success(result);
    }
}