package org.leocoder.picture.controller;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.userreaction.UserReactionRequest;
import org.leocoder.picture.domain.vo.picture.PictureVO;
import org.leocoder.picture.domain.vo.userreaction.UserReactionCountVO;
import org.leocoder.picture.domain.vo.userreaction.UserReactionStatusVO;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.exception.ThrowUtils;
import org.leocoder.picture.service.PictureService;
import org.leocoder.picture.service.UserReactionService;
import org.leocoder.picture.utils.UserContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-16 22:15
 * @description : 用户点赞、踩、收藏、查看等操作请求对象
 */

@RestController
@RequestMapping("/reaction")
@RequiredArgsConstructor
@Api(tags = "用户点赞、踩、收藏、查看等操作相关接口")
@Slf4j
public class UserReactionController {

    private final UserReactionService userReactionService;
    private final PictureService pictureService;

    @PostMapping("/add")
    @ApiOperation(value = "添加用户点赞/收藏（点赞、收藏等）")
    public Result<Boolean> addReaction(@RequestBody UserReactionRequest request) {
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "用户未登录");

        boolean result = userReactionService.addReaction(request, loginUser.getId());
        return ResultUtils.success(result);
    }

    @PostMapping("/remove")
    @ApiOperation(value = "取消用户点赞/收藏")
    public Result<Boolean> removeReaction(@RequestBody UserReactionRequest request) {
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "用户未登录");

        boolean result = userReactionService.removeReaction(request, loginUser.getId());
        return ResultUtils.success(result);
    }

    @GetMapping("/status")
    @ApiOperation(value = "查询用户对目标的点赞/收藏状态")
    public Result<UserReactionStatusVO> getReactionStatus(
            @RequestParam("targetType") String targetType,
            @RequestParam("targetId") Long targetId) {
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "用户未登录");

        UserReactionStatusVO status = userReactionService.getUserReactionStatus(
                targetType, targetId, loginUser.getId());
        return ResultUtils.success(status);
    }

    @PostMapping("/status/batch")
    @ApiOperation(value = "批量查询用户对多个目标的点赞/收藏状态")
    public Result<Map<Long, UserReactionStatusVO>> batchGetReactionStatus(
            @RequestParam("targetType") String targetType,
            @RequestBody List<Long> targetIds) {
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "用户未登录");

        Map<Long, UserReactionStatusVO> statusMap = userReactionService.batchGetUserReactionStatus(
                targetType, targetIds, loginUser.getId());
        return ResultUtils.success(statusMap);
    }

    @GetMapping("/count")
    @ApiOperation(value = "获取目标的点赞/收藏计数")
    public Result<UserReactionCountVO> getReactionCounts(
            @RequestParam("targetType") String targetType,
            @RequestParam("targetId") Long targetId) {
        UserReactionCountVO counts = userReactionService.getReactionCounts(targetType, targetId);
        return ResultUtils.success(counts);
    }

    @PostMapping("/count/batch")
    @ApiOperation(value = "批量获取多个目标的点赞/收藏计数")
    public Result<Map<Long, UserReactionCountVO>> batchGetReactionCounts(
            @RequestParam("targetType") String targetType,
            @RequestBody List<Long> targetIds) {
        Map<Long, UserReactionCountVO> countsMap = userReactionService.batchGetReactionCounts(
                targetType, targetIds);
        return ResultUtils.success(countsMap);
    }

    @GetMapping("/hot/pictures")
    @ApiOperation(value = "获取热门图片列表")
    public Result<List<PictureVO>> getHotPictures(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "period", required = false) String period) {
        List<PictureVO> hotPictures = userReactionService.getHotPictures(limit, period);
        return ResultUtils.success(hotPictures);
    }

    @GetMapping("/user/stats")
    @ApiOperation(value = "获取用户的互动统计")
    public Result<Map<String, Long>> getUserInteractionStats() {
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "用户未登录");

        Map<String, Long> stats = userReactionService.getUserInteractionStats(loginUser.getId());
        return ResultUtils.success(stats);
    }

    @GetMapping("/like")
    @ApiOperation(value = "快捷接口：点赞图片")
    public Result<Boolean> likePicture(@RequestParam("pictureId") Long pictureId) {
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "用户未登录");

        // 构建点赞请求
        UserReactionRequest request = UserReactionRequest.builder()
                .targetType("picture")
                .targetId(pictureId)
                .reactionType("like")
                .build();

        boolean result = userReactionService.addReaction(request, loginUser.getId());
        return ResultUtils.success(result);
    }

    @GetMapping("/unlike")
    @ApiOperation(value = "快捷接口：取消点赞图片")
    public Result<Boolean> unlikePicture(@RequestParam("pictureId") Long pictureId) {
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "用户未登录");

        // 构建取消点赞请求
        UserReactionRequest request = UserReactionRequest.builder()
                .targetType("picture")
                .targetId(pictureId)
                .reactionType("like")
                .build();

        boolean result = userReactionService.removeReaction(request, loginUser.getId());
        return ResultUtils.success(result);
    }

    @GetMapping("/favorite")
    @ApiOperation(value = "快捷接口：收藏图片")
    public Result<Boolean> favoritePicture(@RequestParam("pictureId") Long pictureId) {
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "用户未登录");

        // 构建收藏请求
        UserReactionRequest request = UserReactionRequest.builder()
                .targetType("picture")
                .targetId(pictureId)
                .reactionType("favorite")
                .build();

        boolean result = userReactionService.addReaction(request, loginUser.getId());
        return ResultUtils.success(result);
    }

    @GetMapping("/unfavorite")
    @ApiOperation(value = "快捷接口：取消收藏图片")
    public Result<Boolean> unfavoritePicture(@RequestParam("pictureId") String pictureIdStr) {
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR, "用户未登录");

        // 安全转换为Long类型
        Long pictureId = 0L;
        try {
            pictureId = Long.parseLong(pictureIdStr);
        } catch (NumberFormatException e) {
            ThrowUtils.throwIf(true, ErrorCode.NO_AUTH_ERROR, "图片ID格式错误");
        }

        // 构建取消点赞请求
        UserReactionRequest request = UserReactionRequest.builder()
                .targetType("picture")
                .targetId(pictureId)
                .reactionType("favorite")
                .build();

        boolean result = userReactionService.removeReaction(request, loginUser.getId());
        return ResultUtils.success(result);
    }
}