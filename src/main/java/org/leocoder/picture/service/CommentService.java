package org.leocoder.picture.service;

import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.domain.dto.comment.AdminCommentQueryRequest;
import org.leocoder.picture.domain.dto.comment.CommentAddRequest;
import org.leocoder.picture.domain.dto.comment.CommentReplyAddRequest;
import org.leocoder.picture.domain.pojo.Comment;
import org.leocoder.picture.domain.vo.comment.CommentVO;

import java.util.List;

/**
 * @author : 程序员Leo
 * @date  2025-04-17 22:52
 * @version 1.0
 * @description : 评论服务接口
 */
public interface CommentService {

    /**
     * 添加评论
     *
     * @param commentAddRequest 评论添加请求
     * @param userId 用户ID
     * @return 评论ID
     */
    Long addComment(CommentAddRequest commentAddRequest, Long userId);

    /**
     * 添加评论回复
     *
     * @param commentReplyAddRequest 评论回复添加请求
     * @param userId 用户ID
     * @return 评论ID
     */
    Long addCommentReply(CommentReplyAddRequest commentReplyAddRequest, Long userId);

    /**
     * 删除评论
     *
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否成功
     */
    Boolean deleteComment(Long commentId, Long userId);

    /**
     * 获取内容的评论列表（一级评论）
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param page 页码
     * @param size 每页大小
     * @return 评论列表
     */
    List<CommentVO> getContentComments(String contentType, Long contentId, Integer page, Integer size);

    /**
     * 获取评论的回复列表
     *
     * @param commentId 评论ID
     * @param page 页码
     * @param size 每页大小
     * @return 评论回复列表
     */
    List<CommentVO> getCommentReplies(Long commentId, Integer page, Integer size);

    /**
     * 根据ID获取评论
     *
     * @param commentId 评论ID
     * @return 评论
     */
    Comment getCommentById(Long commentId);

    /**
     * 更新评论状态
     *
     * @param commentId 评论ID
     * @param status 状态
     * @param reviewUserId 审核人ID
     * @param reviewRemark 审核备注
     * @return 是否成功
     */
    Boolean updateCommentStatus(Long commentId, String status, Long reviewUserId, String reviewRemark);


    /**
     * 管理员分页查询评论列表
     *
     * @param commentQueryRequest 查询条件
     * @return 分页评论列表
     */
    PageResult<CommentVO> adminListCommentByPage(AdminCommentQueryRequest commentQueryRequest);

    /**
     * 批量删除评论
     *
     * @param ids 评论ID列表
     * @param userId 操作用户ID
     * @return 是否成功
     */
    Boolean batchDeleteComments(List<Long> ids, Long userId);


    /**
     * 设置评论为置顶/取消置顶
     *
     * @param commentId 评论ID
     * @param isTop 是否置顶
     * @param userId 操作用户ID
     * @return 是否成功
     */
    Boolean updateCommentTopStatus(Long commentId, Boolean isTop, Long userId);

    /**
     * 设置评论为热门/取消热门
     *
     * @param commentId 评论ID
     * @param isHot 是否热门
     * @param userId 操作用户ID
     * @return 是否成功
     */
    Boolean updateCommentHotStatus(Long commentId, Boolean isHot, Long userId);


    /**
     * 获取内容的评论树结构
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 评论树结构
     */
    List<CommentVO> getContentCommentsTree(String contentType, Long contentId);
}