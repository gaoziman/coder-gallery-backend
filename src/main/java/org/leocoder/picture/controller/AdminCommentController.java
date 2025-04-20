package org.leocoder.picture.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.domain.dto.comment.AdminCommentQueryRequest;
import org.leocoder.picture.domain.dto.comment.BatchDeleteCommentRequest;
import org.leocoder.picture.domain.dto.comment.CommentAddRequest;
import org.leocoder.picture.domain.dto.comment.CommentReplyAddRequest;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.comment.CommentVO;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.service.CommentService;
import org.leocoder.picture.service.UserService;
import org.leocoder.picture.utils.UserContext;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-17 23:00
 * @description : 管理员评论相关接口
 */
@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Api(tags = "管理员评论相关接口")
@Slf4j
public class AdminCommentController {

    private final CommentService commentService;
    private final UserService userService;

    @ApiOperation("管理员添加评论")
    @PostMapping("/add")
    public Result<Long> addComment(@RequestBody @Validated CommentAddRequest commentAddRequest) {
        // 参数校验
        if (ObjectUtil.isNull(commentAddRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        if (StrUtil.isBlank(commentAddRequest.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        }

        if (StrUtil.isBlank(commentAddRequest.getContentType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论对象类型不能为空");
        }

        if (ObjectUtil.isNull(commentAddRequest.getContentId()) || commentAddRequest.getContentId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论对象ID不合法");
        }

        // 获取当前登录用户
        User loginUser = UserContext.getUser();
        if (ObjectUtil.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先登录");
        }

        // 检查管理员权限
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }

        Long commentId = commentService.addComment(commentAddRequest, loginUser.getId());
        return ResultUtils.success(commentId);
    }

    @ApiOperation("管理员添加评论回复")
    @PostMapping("/reply")
    public Result<Long> addCommentReply(@RequestBody @Validated CommentReplyAddRequest commentReplyAddRequest) {
        // 参数校验
        if (ObjectUtil.isNull(commentReplyAddRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        if (StrUtil.isBlank(commentReplyAddRequest.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回复内容不能为空");
        }

        if (ObjectUtil.isNull(commentReplyAddRequest.getParentId()) || commentReplyAddRequest.getParentId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "父评论ID不合法");
        }

        // 获取当前登录用户
        User loginUser = UserContext.getUser();
        if (ObjectUtil.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先登录");
        }

        // 检查管理员权限
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }

        Long commentId = commentService.addCommentReply(commentReplyAddRequest, loginUser.getId());
        return ResultUtils.success(commentId);
    }

    @ApiOperation("管理员删除评论")
    @DeleteMapping("/delete")
    public Result<Boolean> deleteComment(@RequestParam("commentId") Long commentId) {
        // 参数校验
        if (ObjectUtil.isNull(commentId) || commentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论ID不合法");
        }

        // 获取当前登录用户
        User loginUser = UserContext.getUser();
        if (ObjectUtil.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先登录");
        }

        // 检查管理员权限
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }

        Boolean result = commentService.deleteComment(commentId, loginUser.getId());
        return ResultUtils.success(result);
    }

    @ApiOperation("管理员分页查询评论列表")
    @GetMapping("/list/page")
    public Result<PageResult<CommentVO>> listCommentByPage(AdminCommentQueryRequest commentQueryRequest) {
        // 参数校验
        if (ObjectUtil.isNull(commentQueryRequest)) {
            commentQueryRequest = new AdminCommentQueryRequest();
        }

        // 校验分页参数
        Integer pageNum = commentQueryRequest.getPageNum();
        Integer pageSize = commentQueryRequest.getPageSize();

        if (ObjectUtil.isNotNull(pageNum) && pageNum <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }

        if (ObjectUtil.isNotNull(pageSize) && (pageSize <= 0 || pageSize > 100)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页条数必须在1-100之间");
        }

        // 获取当前登录用户
        User loginUser = UserContext.getUser();
        if (ObjectUtil.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先登录");
        }

        // 检查管理员权限
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }

        PageResult<CommentVO> pageResult = commentService.adminListCommentByPage(commentQueryRequest);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("管理员批量删除评论")
    @DeleteMapping("/batch/delete")
    public Result<Boolean> batchDeleteComments(@RequestBody BatchDeleteCommentRequest requestParam) {
        // 参数校验
        if (ObjectUtil.isNull(requestParam)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        if (ObjectUtil.isNull(requestParam.getIds()) || CollUtil.isEmpty(requestParam.getIds())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论ID列表不能为空");
        }

        // 校验ID列表中的值是否合法
        for (Long id : requestParam.getIds()) {
            if (ObjectUtil.isNull(id) || id <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论ID列表中包含不合法的ID");
            }
        }

        // 获取当前登录用户
        User loginUser = UserContext.getUser();
        if (ObjectUtil.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先登录");
        }

        // 检查管理员权限
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }

        Boolean result = commentService.batchDeleteComments(requestParam.getIds(), loginUser.getId());
        return ResultUtils.success(result);
    }

    @ApiOperation("管理员更新评论状态")
    @PostMapping("/status")
    public Result<Boolean> updateCommentStatus(
            @RequestParam("commentId") Long commentId,
            @RequestParam("status") String status,
            @RequestParam(value = "reviewRemark", required = false) String reviewRemark) {

        // 参数校验
        if (ObjectUtil.isNull(commentId) || commentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论ID不合法");
        }

        if (StrUtil.isBlank(status)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论状态不能为空");
        }

        // 验证状态值是否合法（可以增加状态枚举校验）
        if (!status.equals("pending") && !status.equals("approved") &&
                !status.equals("rejected") && !status.equals("reported")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论状态值不合法");
        }

        // 获取当前登录用户
        User loginUser = UserContext.getUser();
        if (ObjectUtil.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先登录");
        }

        // 检查管理员权限
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }

        Boolean result = commentService.updateCommentStatus(commentId, status, loginUser.getId(), reviewRemark);
        return ResultUtils.success(result);
    }

    @ApiOperation("设置评论置顶状态")
    @PostMapping("/top")
    public Result<Boolean> setCommentTopStatus(
            @RequestParam("commentId") Long commentId,
            @RequestParam("isTop") Boolean isTop) {

        // 参数校验
        if (ObjectUtil.isNull(commentId) || commentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论ID不合法");
        }

        if (ObjectUtil.isNull(isTop)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "置顶状态不能为空");
        }

        // 获取当前登录用户
        User loginUser = UserContext.getUser();
        if (ObjectUtil.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先登录");
        }

        // 检查管理员权限
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }

        Boolean result = commentService.updateCommentTopStatus(commentId, isTop, loginUser.getId());
        return ResultUtils.success(result);
    }

    @ApiOperation("设置评论热门状态")
    @PostMapping("/hot")
    public Result<Boolean> setCommentHotStatus(
            @RequestParam("commentId") Long commentId,
            @RequestParam("isHot") Boolean isHot) {

        // 参数校验
        if (ObjectUtil.isNull(commentId) || commentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论ID不合法");
        }

        if (ObjectUtil.isNull(isHot)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "热门状态不能为空");
        }

        // 获取当前登录用户
        User loginUser = UserContext.getUser();
        if (ObjectUtil.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先登录");
        }

        // 检查管理员权限
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }

        Boolean result = commentService.updateCommentHotStatus(commentId, isHot, loginUser.getId());
        return ResultUtils.success(result);
    }
}