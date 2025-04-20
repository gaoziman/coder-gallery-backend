package org.leocoder.picture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.common.PageRequest;
import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.common.PageUtils;
import org.leocoder.picture.domain.dto.comment.AdminCommentQueryRequest;
import org.leocoder.picture.domain.dto.comment.CommentAddRequest;
import org.leocoder.picture.domain.dto.comment.CommentReplyAddRequest;
import org.leocoder.picture.domain.mapstruct.CommentConvert;
import org.leocoder.picture.domain.pojo.Comment;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.comment.CommentVO;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.exception.ThrowUtils;
import org.leocoder.picture.mapper.CommentMapper;
import org.leocoder.picture.mapper.UserMapper;
import org.leocoder.picture.service.CommentService;
import org.leocoder.picture.service.UserService;
import org.leocoder.picture.utils.SnowflakeIdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : 程序员Leo
 * @date  2025-04-17 22:52
 * @version 1.0
 * @description : 评论服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;

    private final UserService userService;

    private final UserMapper userMapper;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 添加评论
     *
     * @param commentAddRequest 评论添加请求
     * @param userId 用户ID
     * @return 评论ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(CommentAddRequest commentAddRequest, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(commentAddRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjectUtil.isNull(userId), ErrorCode.NO_AUTH_ERROR);

        String content = commentAddRequest.getContent();
        String contentType = commentAddRequest.getContentType();
        Long contentId = commentAddRequest.getContentId();

        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(contentType), ErrorCode.PARAMS_ERROR, "评论对象类型不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(contentId) || contentId <= 0, ErrorCode.PARAMS_ERROR, "评论对象ID不能为空");

        // 创建评论
        Comment comment = new Comment();
        comment.setId(snowflakeIdGenerator.nextId());
        comment.setContent(content);
        comment.setContentType(contentType);
        comment.setContentId(contentId);
        // 一级评论，父评论ID和根评论ID为空
        comment.setParentId(null);
        comment.setRootId(null);
        comment.setReplyUserId(null);
        comment.setReplyCount(0);
        comment.setLikeCount(0);
        // 默认状态为待审核
        comment.setStatus("pending");
        comment.setIsTop(false);
        comment.setIsHot(false);
        // 创建信息
        comment.setCreateTime(LocalDateTime.now());
        comment.setCreateUser(userId);
        comment.setIsDeleted(0);

        try {
            // 插入数据库
            int result = commentMapper.insertWithId(comment);
            ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR, "评论失败");

            log.info("用户 {} 添加评论成功, ID: {}", userId, comment.getId());
            return comment.getId();
        } catch (Exception e) {
            log.error("添加评论失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "评论失败: " + e.getMessage());
        }
    }

    /**
     * 添加评论回复
     *
     * @param commentReplyAddRequest 评论回复添加请求
     * @param userId 用户ID
     * @return 评论ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addCommentReply(CommentReplyAddRequest commentReplyAddRequest, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(commentReplyAddRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjectUtil.isNull(userId), ErrorCode.NO_AUTH_ERROR);

        String content = commentReplyAddRequest.getContent();
        Long parentId = commentReplyAddRequest.getParentId();
        Long replyUserId = commentReplyAddRequest.getReplyUserId();

        ThrowUtils.throwIf(content == null || content.isEmpty(), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        ThrowUtils.throwIf(parentId == null || parentId <= 0, ErrorCode.PARAMS_ERROR, "父评论ID不能为空");

        // 查询父评论是否存在
        Comment parentComment = commentMapper.selectById(parentId);
        ThrowUtils.throwIf(parentComment == null, ErrorCode.NOT_FOUND_ERROR, "父评论不存在");
        ThrowUtils.throwIf( parentComment.isDeleted(), ErrorCode.PARAMS_ERROR, "父评论已被删除");

        // 确定根评论ID
        Long rootId;
        if (parentComment.getRootId() != null) {
            // 如果父评论有根评论ID，说明父评论是子评论，用它的根评论ID
            rootId = parentComment.getRootId();
        } else {
            // 否则父评论就是根评论，用它的ID作为根评论ID
            rootId = parentComment.getId();
        }

        // 创建评论回复
        Comment reply = new Comment();
        reply.setId(snowflakeIdGenerator.nextId());
        reply.setContent(content);
        // 使用与父评论相同的内容类型和内容ID
        reply.setContentType(parentComment.getContentType());
        reply.setContentId(parentComment.getContentId());
        reply.setParentId(parentId);
        reply.setRootId(rootId);
        reply.setReplyUserId(replyUserId);
        reply.setReplyCount(0);
        reply.setLikeCount(0);
        // 默认状态为待审核
        reply.setStatus("pending");
        reply.setIsTop(false);
        reply.setIsHot(false);
        // 创建信息
        reply.setCreateTime(LocalDateTime.now());
        reply.setCreateUser(userId);
        reply.setIsDeleted(0);

        try {
            // 插入数据库
            int result = commentMapper.insertWithId(reply);
            ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR, "回复失败");

            // 增加父评论的回复数
            commentMapper.incrementReplyCount(parentId);
            // 如果父评论不是根评论，也增加根评论的回复数
            if (!parentId.equals(rootId)) {
                commentMapper.incrementReplyCount(rootId);
            }

            log.info("用户 {} 添加评论回复成功, ID: {}", userId, reply.getId());
            return reply.getId();
        } catch (Exception e) {
            log.error("添加评论回复失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "回复失败: " + e.getMessage());
        }
    }

    /**
     * 删除评论
     *
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteComment(Long commentId, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(commentId == null || commentId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userId == null, ErrorCode.NO_AUTH_ERROR);

        // 查询评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        ThrowUtils.throwIf(comment == null, ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        ThrowUtils.throwIf(comment.getIsDeleted() == 1, ErrorCode.PARAMS_ERROR, "评论已被删除");

        // 检查权限 - 只有评论作者和管理员可以删除
        User loginUser = userMapper.selectById(userId);
        boolean isAdmin = userService.isAdmin(loginUser);
        boolean isAuthor = comment.getCreateUser().equals(userId);
        ThrowUtils.throwIf(!isAdmin && !isAuthor, ErrorCode.NO_AUTH_ERROR, "无权删除此评论");

        try {
            // 逻辑删除评论
            int result = commentMapper.markAsDeleted(commentId, userId);
            ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR, "删除评论失败");

            // 如果是回复，需要减少父评论和根评论的回复数
            if (comment.getParentId() != null) {
                // 减少父评论的回复数
                commentMapper.decrementReplyCount(comment.getParentId());

                // 如果父评论不是根评论，也减少根评论的回复数
                if (!comment.getParentId().equals(comment.getRootId())) {
                    commentMapper.decrementReplyCount(comment.getRootId());
                }
            }

            log.info("用户 {} 删除评论成功, 评论ID: {}", userId, commentId);
            return true;
        } catch (Exception e) {
            log.error("删除评论失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除评论失败: " + e.getMessage());
        }
    }

    /**
     * 获取内容的评论列表（一级评论）
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param page 页码
     * @param size 每页大小
     * @return 评论列表
     */
    @Override
    public List<CommentVO> getContentComments(String contentType, Long contentId, Integer page, Integer size) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(contentType), ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(contentId) || contentId <= 0, ErrorCode.PARAMS_ERROR, "内容ID不能为空");

        try {
            // 创建一个简单的分页请求对象
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPageNum(ObjectUtil.isNull(page) || page < 1 ? 1 : page);
            pageRequest.setPageSize(ObjectUtil.isNull(size) || size < 1 || size > 100 ? 10 : size);

            // 使用PageUtils进行分页查询
            PageResult<CommentVO> pageResult = PageUtils.doPage(
                    pageRequest,
                    () -> commentMapper.selectContentComments(
                            contentType,
                            contentId
                    ),
                    this::convertToCommentVO
            );

            log.info("获取内容评论成功: 内容类型={}, 内容ID={}, 评论数={}", contentType, contentId, pageResult.getRecords().size());
            return pageResult.getRecords();
        } catch (Exception e) {
            log.error("获取内容评论失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取评论失败: " + e.getMessage());
        }
    }

    /**
     * 获取评论的回复列表
     */
    @Override
    public List<CommentVO> getCommentReplies(Long commentId, Integer page, Integer size) {
        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(commentId) || commentId <= 0, ErrorCode.PARAMS_ERROR, "评论ID不能为空");

        // 查询评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        ThrowUtils.throwIf(ObjectUtil.isNull(comment), ErrorCode.NOT_FOUND_ERROR, "评论不存在");

        // 如果是子评论，应该查询其根评论的所有回复
        Long rootId = ObjectUtil.isNotNull(comment.getRootId()) ? comment.getRootId() : comment.getId();

        try {
            // 创建一个简单的分页请求对象
            PageRequest pageRequest = new PageRequest();
            pageRequest.setPageNum(ObjectUtil.isNull(page) || page < 1 ? 1 : page);
            pageRequest.setPageSize(ObjectUtil.isNull(size) || size < 1 || size > 100 ? 10 : size);

            // 使用PageUtils进行分页查询，PageHelper会自动处理分页
            PageResult<CommentVO> pageResult = PageUtils.doPage(
                    pageRequest,
                    () -> commentMapper.selectCommentReplies(rootId),
                    this::convertToCommentVO
            );

            log.info("获取评论回复成功: 评论ID={}, 回复数={}", commentId, pageResult.getRecords().size());
            return pageResult.getRecords();
        } catch (Exception e) {
            log.error("获取评论回复失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取回复失败: " + e.getMessage());
        }
    }
    /**
     * 根据ID获取评论
     *
     * @param commentId 评论ID
     * @return 评论
     */
    @Override
    public Comment getCommentById(Long commentId) {
        ThrowUtils.throwIf(commentId == null || commentId <= 0, ErrorCode.PARAMS_ERROR);
        return commentMapper.selectById(commentId);
    }

    /**
     * 更新评论状态
     *
     * @param commentId 评论ID
     * @param status 状态
     * @param reviewUserId 审核人ID
     * @param reviewRemark 审核备注
     * @return 是否成功
     */
    @Override
    public Boolean updateCommentStatus(Long commentId, String status, Long reviewUserId, String reviewRemark) {
        // 参数校验
        ThrowUtils.throwIf(commentId == null || commentId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(status == null || status.isEmpty(), ErrorCode.PARAMS_ERROR, "状态不能为空");

        // 检查评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        ThrowUtils.throwIf(comment == null, ErrorCode.NOT_FOUND_ERROR, "评论不存在");

        // 更新评论状态
        Comment updateComment = new Comment();
        updateComment.setId(commentId);
        updateComment.setStatus(status);
        updateComment.setReviewUserId(reviewUserId);
        updateComment.setReviewRemark(reviewRemark);
        updateComment.setReviewTime(LocalDateTime.now());
        updateComment.setUpdateTime(LocalDateTime.now());
        updateComment.setUpdateUser(reviewUserId);

        try {
            int result = commentMapper.updateByPrimaryKeySelective(updateComment);
            ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR, "更新评论状态失败");

            log.info("评论状态更新成功: 评论ID={}, 状态={}, 审核人={}", commentId, status, reviewUserId);
            return true;
        } catch (Exception e) {
            log.error("更新评论状态失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新评论状态失败: " + e.getMessage());
        }
    }

    /**
     * 将评论实体转换为VO，并填充用户信息
     *
     * @param comment 评论实体
     * @return 评论VO
     */
    private CommentVO convertToCommentVO(Comment comment) {
        if (ObjectUtil.isNull(comment)) {
            return null;
        }

        // 使用MapStruct转换为VO
        CommentVO commentVO = CommentConvert.INSTANCE.toCommentVO(comment);

        try {
            // 填充评论者信息
            if (ObjectUtil.isNotNull(comment.getCreateUser())) {
                commentVO.setUser(userService.getUserById(comment.getCreateUser()));
            }

            // 填充被回复者信息
            if (ObjectUtil.isNotNull(comment.getReplyUserId())) {
                commentVO.setReplyUser(userService.getUserById(comment.getReplyUserId()));
            }
        } catch (Exception e) {
            log.warn("获取评论用户信息失败: commentId={}", comment.getId(), e);
        }

        return commentVO;
    }


    /**
     * 管理员分页查询评论列表
     *
     * @param commentQueryRequest 查询条件
     * @return 分页评论列表
     */
    @Override
    public PageResult<CommentVO> adminListCommentByPage(AdminCommentQueryRequest commentQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(commentQueryRequest), ErrorCode.PARAMS_ERROR);

        // 使用PageUtils进行分页查询
        return PageUtils.doPage(
                commentQueryRequest,
                () -> commentMapper.adminListComments(
                        commentQueryRequest,
                        commentQueryRequest.getCreateTimeStart(),
                        commentQueryRequest.getCreateTimeEnd()
                ),
                comment -> {
                    // 使用MapStruct转换为VO
                    CommentVO commentVO = CommentConvert.INSTANCE.toCommentVO(comment);

                    // 填充用户信息
                    try {
                        if (comment.getCreateUser() != null) {
                            commentVO.setUser(userService.getUserVOById(comment.getCreateUser()));
                        }

                        if (comment.getReplyUserId() != null) {
                            commentVO.setReplyUser(userService.getUserVOById(comment.getReplyUserId()));
                        }
                    } catch (Exception e) {
                        log.warn("获取评论用户信息失败: commentId={}", comment.getId(), e);
                    }

                    return commentVO;
                }
        );
    }

    /**
     * 批量删除评论
     *
     * @param ids 评论ID列表
     * @param userId 操作用户ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDeleteComments(List<Long> ids, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(ids), ErrorCode.PARAMS_ERROR, "评论ID列表不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(userId), ErrorCode.NO_AUTH_ERROR);

        try {
            // 批量逻辑删除评论
            int result = commentMapper.batchLogicDeleteComments(ids, userId);

            log.info("批量删除评论成功: 用户ID={}, 评论ID列表={}, 影响行数={}", userId, ids, result);
            return true;
        } catch (Exception e) {
            log.error("批量删除评论失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量删除评论失败: " + e.getMessage());
        }
    }


    /**
     * 设置评论为置顶/取消置顶
     *
     * @param commentId 评论ID
     * @param isTop 是否置顶
     * @param userId 操作用户ID
     * @return 是否成功
     */
    @Override
    public Boolean updateCommentTopStatus(Long commentId, Boolean isTop, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(commentId == null || commentId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(isTop == null, ErrorCode.PARAMS_ERROR, "置顶状态不能为空");
        ThrowUtils.throwIf(userId == null, ErrorCode.NO_AUTH_ERROR);

        // 检查评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        ThrowUtils.throwIf(comment == null, ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        ThrowUtils.throwIf( comment.isDeleted(), ErrorCode.PARAMS_ERROR, "评论已被删除");

        // 只能置顶一级评论
        if (isTop && comment.getParentId() != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只能置顶一级评论");
        }

        // 更新评论置顶状态
        Comment updateComment = new Comment();
        updateComment.setId(commentId);
        updateComment.setIsTop(isTop);
        updateComment.setUpdateTime(LocalDateTime.now());
        updateComment.setUpdateUser(userId);

        try {
            int result = commentMapper.updateByPrimaryKeySelective(updateComment);
            ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR, "更新评论置顶状态失败");

            log.info("更新评论置顶状态成功: 评论ID={}, 置顶状态={}, 操作用户={}", commentId, isTop, userId);
            return true;
        } catch (Exception e) {
            log.error("更新评论置顶状态失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新评论置顶状态失败: " + e.getMessage());
        }
    }

    /**
     * 设置评论为热门/取消热门
     *
     * @param commentId 评论ID
     * @param isHot 是否热门
     * @param userId 操作用户ID
     * @return 是否成功
     */
    @Override
    public Boolean updateCommentHotStatus(Long commentId, Boolean isHot, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(commentId == null || commentId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(isHot == null, ErrorCode.PARAMS_ERROR, "热门状态不能为空");
        ThrowUtils.throwIf(userId == null, ErrorCode.NO_AUTH_ERROR);

        // 检查评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        ThrowUtils.throwIf(comment == null, ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        ThrowUtils.throwIf( comment.isDeleted(), ErrorCode.PARAMS_ERROR, "评论已被删除");

        // 更新评论热门状态
        Comment updateComment = new Comment();
        updateComment.setId(commentId);
        updateComment.setIsHot(isHot);
        updateComment.setUpdateTime(LocalDateTime.now());
        updateComment.setUpdateUser(userId);

        try {
            int result = commentMapper.updateByPrimaryKeySelective(updateComment);
            ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR, "更新评论热门状态失败");

            log.info("更新评论热门状态成功: 评论ID={}, 热门状态={}, 操作用户={}", commentId, isHot, userId);
            return true;
        } catch (Exception e) {
            log.error("更新评论热门状态失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新评论热门状态失败: " + e.getMessage());
        }
    }


    /**
     * 获取内容的评论树结构
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 评论树结构
     */
    @Override
    public List<CommentVO> getContentCommentsTree(String contentType, Long contentId) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(contentType), ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(contentId) || contentId <= 0, ErrorCode.PARAMS_ERROR, "内容ID不能为空");

        try {
            // 1. 获取所有根评论（一级评论）
            List<Comment> rootComments = commentMapper.selectContentComments(contentType, contentId);
            if (CollUtil.isEmpty(rootComments)) {
                return new ArrayList<>();
            }

            // 2. 提取根评论ID
            Set<Long> rootIds = rootComments.stream()
                    .map(Comment::getId)
                    .collect(Collectors.toSet());

            // 3. 获取这些根评论下的所有回复
            List<Comment> allReplies = new ArrayList<>();
            if (!rootIds.isEmpty()) {
                allReplies = commentMapper.selectAllRepliesByRootIds(rootIds);
            }

            // 4. 构建评论树
            return buildCommentTree(rootComments, allReplies);
        } catch (Exception e) {
            log.error("获取评论树失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取评论失败: " + e.getMessage());
        }
    }

    /**
     * 构建评论树
     *
     * @param rootComments 根评论列表
     * @param allReplies 所有回复
     * @return 评论树
     */
    private List<CommentVO> buildCommentTree(List<Comment> rootComments, List<Comment> allReplies) {
        // 1. 将所有回复按parentId分组
        Map<Long, List<Comment>> replyMap = allReplies.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 2. 递归构建评论树
        return rootComments.stream()
                .map(root -> {
                    CommentVO rootVO = convertToCommentVO(root);
                    rootVO.setChildren(buildChildrenTree(root.getId(), replyMap));
                    return rootVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 递归构建子评论树
     *
     * @param parentId 父评论ID
     * @param replyMap 回复映射
     * @return 子评论树
     */
    private List<CommentVO> buildChildrenTree(Long parentId, Map<Long, List<Comment>> replyMap) {
        // 获取当前父评论下的直接回复
        List<Comment> children = replyMap.getOrDefault(parentId, Collections.emptyList());

        // 如果没有直接回复，返回空列表
        if (children.isEmpty()) {
            return Collections.emptyList();
        }

        // 递归构建每个子评论的子树
        return children.stream()
                .map(child -> {
                    CommentVO childVO = convertToCommentVO(child);
                    childVO.setChildren(buildChildrenTree(child.getId(), replyMap));
                    return childVO;
                })
                .collect(Collectors.toList());
    }
}