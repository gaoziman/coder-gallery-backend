package org.leocoder.picture.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.domain.dto.picture.AdminPictureQueryRequest;
import org.leocoder.picture.domain.dto.picture.PictureReviewRequest;
import org.leocoder.picture.domain.vo.picture.PictureStatisticsVO;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.picture.PictureVO;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.exception.ThrowUtils;
import org.leocoder.picture.service.PictureService;
import org.leocoder.picture.service.UserService;
import org.leocoder.picture.utils.UserContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-25 08:30
 * @description : 图片后台管理接口
 */
@RestController
@RequestMapping("/admin/picture")
@RequiredArgsConstructor
@Api(tags = "图片后台管理接口")
@Slf4j
public class AdminPictureController {

    private final PictureService pictureService;
    private final UserService userService;

    @ApiOperation("管理员分页查询图片列表")
    @GetMapping("/list/page")
    public Result<PageResult<PictureVO>> listPicturesByPage(AdminPictureQueryRequest queryRequest) {
        // 参数校验
        if (queryRequest == null) {
            queryRequest = new AdminPictureQueryRequest();
        }

        // 校验分页参数
        Integer pageNum = queryRequest.getPageNum();
        Integer pageSize = queryRequest.getPageSize();

        if (pageNum != null && pageNum <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }

        if (pageSize != null && (pageSize <= 0 || pageSize > 100)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页条数必须在1-100之间");
        }

        // 获取当前登录用户
        User loginUser = UserContext.getUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先登录");
        }

        // 检查管理员权限
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }

        // 调用服务层方法进行分页查询
        PageResult<PictureVO> pageResult = pictureService.adminListPicturesByPage(queryRequest);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation(value = "删除图片")
    @DeleteMapping("/{id}")
    public Result<Boolean> deletePicture(@PathVariable("id") Long id) {
        // 获取当前登录用户并检查管理员权限
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "未登录");
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "非管理员禁止访问");

        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(id) || id <= 0, ErrorCode.PARAMS_ERROR, "无效的图片ID");

        // 调用服务层方法删除图片
        Boolean result = pictureService.adminDeletePicture(id, loginUser.getId());

        return ResultUtils.success(result);
    }

    @ApiOperation(value = "批量删除图片")
    @DeleteMapping("/batch")
    public Result<Boolean> batchDeletePictures(@RequestBody List<Long> ids) {
        // 获取当前登录用户并检查管理员权限
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "未登录");
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "非管理员禁止访问");

        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(ids), ErrorCode.PARAMS_ERROR, "图片ID列表不能为空");

        // 调用服务层方法批量删除图片
        Boolean result = pictureService.adminBatchDeletePictures(ids, loginUser.getId());

        return ResultUtils.success(result);
    }

    @ApiOperation(value = "审核图片")
    @PostMapping("/review")
    public Result<Boolean> reviewPicture(@RequestBody PictureReviewRequest reviewRequest) {
        // 获取当前登录用户并检查管理员权限
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "未登录");
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "非管理员禁止访问");

        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(reviewRequest), ErrorCode.PARAMS_ERROR, "审核参数不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(reviewRequest.getPictureId()) || reviewRequest.getPictureId() <= 0,
                ErrorCode.PARAMS_ERROR, "无效的图片ID");
        ThrowUtils.throwIf(ObjectUtil.isNull(reviewRequest.getReviewStatus()),
                ErrorCode.PARAMS_ERROR, "审核状态不能为空");

        // 调用服务层方法审核图片
        Boolean result = pictureService.adminReviewPicture(reviewRequest, loginUser.getId());

        return ResultUtils.success(result);
    }

    @ApiOperation(value = "批量审核图片")
    @PostMapping("/review/batch")
    public Result<Boolean> batchReviewPictures(@RequestBody PictureReviewRequest reviewRequest) {
        // 获取当前登录用户并检查管理员权限
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "未登录");
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "非管理员禁止访问");

        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(reviewRequest), ErrorCode.PARAMS_ERROR, "审核参数不能为空");
        ThrowUtils.throwIf(CollUtil.isEmpty(reviewRequest.getPictureIds()),
                ErrorCode.PARAMS_ERROR, "图片ID列表不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(reviewRequest.getReviewStatus()),
                ErrorCode.PARAMS_ERROR, "审核状态不能为空");

        // 调用服务层方法批量审核图片
        Boolean result = pictureService.adminBatchReviewPictures(reviewRequest, loginUser.getId());

        return ResultUtils.success(result);
    }

    @ApiOperation("获取图片统计数据")
    @GetMapping("/statistics")
    public Result<PictureStatisticsVO> getPictureStatistics() {
        // 获取当前登录用户
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "未登录");

        // 检查管理员权限
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "非管理员禁止访问");

        PictureStatisticsVO statistics = pictureService.getPictureStatistics();

        return ResultUtils.success(statistics);
    }
}