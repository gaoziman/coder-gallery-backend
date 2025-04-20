package org.leocoder.picture.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.domain.dto.comment.CommentAddRequest;
import org.leocoder.picture.domain.dto.comment.CommentReplyAddRequest;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.comment.CommentVO;
import org.leocoder.picture.enums.CommentStatusEnum;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.service.CommentService;
import org.leocoder.picture.utils.UserContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 评论相关接口
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "评论相关接口")
@Slf4j
@RequestMapping("/comment")
public class CommentController {

    private final CommentService commentService;

    @ApiOperation("添加评论")
    @PostMapping("/add")
    public Result<Long> addComment(@RequestBody CommentAddRequest commentAddRequest) {
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

        Long commentId = commentService.addComment(commentAddRequest, loginUser.getId());
        return ResultUtils.success(commentId);
    }

    @ApiOperation("添加评论回复")
    @PostMapping("/reply")
    public Result<Long> addCommentReply(@RequestBody  CommentReplyAddRequest commentReplyAddRequest) {
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

        Long commentId = commentService.addCommentReply(commentReplyAddRequest, loginUser.getId());
        return ResultUtils.success(commentId);
    }

    @ApiOperation("删除评论")
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

        Boolean result = commentService.deleteComment(commentId, loginUser.getId());
        return ResultUtils.success(result);
    }

    // @ApiOperation("获取内容评论列表")
    // @GetMapping("/list")
    // public Result<List<CommentVO>> getContentComments(
    //         @RequestParam("contentType") String contentType,
    //         @RequestParam("contentId") Long contentId,
    //         @RequestParam(value = "page", defaultValue = "1") Integer page,
    //         @RequestParam(value = "size", defaultValue = "10") Integer size) {
    //
    //     // 参数校验
    //     if (StrUtil.isBlank(contentType)) {
    //         throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
    //     }
    //
    //     if (ObjectUtil.isNull(contentId) || contentId <= 0) {
    //         throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不合法");
    //     }
    //
    //     if (ObjectUtil.isNull(page) || page < 1) {
    //         page = 1;
    //     }
    //
    //     if (ObjectUtil.isNull(size) || size < 1 || size > 100) {
    //         size = 10;
    //     }
    //
    //     List<CommentVO> commentList = commentService.getContentComments(contentType, contentId, page, size);
    //     return ResultUtils.success(commentList);
    // }

    // @ApiOperation("获取评论回复列表")
    // @GetMapping("/{commentId}/replies")
    // public Result<List<CommentVO>> getCommentReplies(
    //         @PathVariable("commentId") Long commentId,
    //         @RequestParam(value = "page", defaultValue = "1") Integer page,
    //         @RequestParam(value = "size", defaultValue = "10") Integer size) {
    //
    //     // 参数校验
    //     if (ObjectUtil.isNull(commentId) || commentId <= 0) {
    //         throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论ID不合法");
    //     }
    //
    //     if (ObjectUtil.isNull(page) || page < 1) {
    //         page = 1;
    //     }
    //
    //     if (ObjectUtil.isNull(size) || size < 1 || size > 100) {
    //         size = 10;
    //     }
    //
    //     List<CommentVO> replyList = commentService.getCommentReplies(commentId, page, size);
    //     return ResultUtils.success(replyList);
    // }

    @ApiOperation("更新评论状态")
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

        // 使用枚举验证状态值是否合法
        if (!CommentStatusEnum.isValidStatus(status)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论状态值不合法");
        }

        // 获取当前登录用户
        User loginUser = UserContext.getUser();
        if (ObjectUtil.isNull(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先登录");
        }

        Boolean result = commentService.updateCommentStatus(commentId, status, loginUser.getId(), reviewRemark);
        return ResultUtils.success(result);
    }


    @ApiOperation("获取内容评论树")
    @GetMapping("/tree")
    public Result<List<CommentVO>> getContentCommentsTree(
            @RequestParam("contentType") String contentType,
            @RequestParam("contentId") Long contentId) {

        // 参数校验
        if (StrUtil.isBlank(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }

        if (ObjectUtil.isNull(contentId) || contentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不合法");
        }

        List<CommentVO> commentTree = commentService.getContentCommentsTree(contentType, contentId);
        return ResultUtils.success(commentTree);
    }
}