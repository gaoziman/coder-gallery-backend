package org.leocoder.picture.mapper;

import org.apache.ibatis.annotations.Param;
import org.leocoder.picture.domain.dto.comment.AdminCommentQueryRequest;
import org.leocoder.picture.domain.pojo.Comment;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-17 22:52
 * @description : 评论Mapper
 */
public interface CommentMapper {
    int deleteById(Long id);

    int insert(Comment record);

    /**
     * 使用指定ID插入评论
     *
     * @param record 评论对象
     * @return 影响行数
     */
    int insertWithId(Comment record);

    int insertSelective(Comment record);

    Comment selectById(Long id);

    int updateByPrimaryKeySelective(Comment record);

    int updateById(Comment record);

    /**
     * 查询内容的一级评论列表
     *
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 评论列表
     */
    List<Comment> selectContentComments(
            @Param("contentType") String contentType,
            @Param("contentId") Long contentId
    );

    /**
     * 查询评论的回复列表
     *
     * @param rootId 根评论ID
     * @return 评论回复列表
     */
    List<Comment> selectCommentReplies(
            @Param("rootId") Long rootId
    );

    /**
     * 统计内容的评论数量
     *
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 评论数量
     */
    int countContentComments(
            @Param("contentType") String contentType,
            @Param("contentId") Long contentId
    );

    /**
     * 统计评论的回复数量
     *
     * @param rootId 根评论ID
     * @return 回复数量
     */
    int countCommentReplies(@Param("rootId") Long rootId);

    /**
     * 增加评论回复数
     *
     * @param commentId 评论ID
     * @return 影响行数
     */
    int incrementReplyCount(@Param("commentId") Long commentId);

    /**
     * 减少评论回复数
     *
     * @param commentId 评论ID
     * @return 影响行数
     */
    int decrementReplyCount(@Param("commentId") Long commentId);

    /**
     * 标记评论为逻辑删除
     *
     * @param commentId 评论ID
     * @param userId    更新用户ID
     * @return 影响行数
     */
    int markAsDeleted(@Param("commentId") Long commentId, @Param("userId") Long userId);


    /**
     * 管理员分页查询评论列表
     *
     * @param queryRequest 查询条件
     * @param startTime    创建时间起始
     * @param endTime      创建时间截止
     * @return 评论列表
     */
    List<Comment> adminListComments(
            @Param("query") AdminCommentQueryRequest queryRequest,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 批量标记评论为逻辑删除
     *
     * @param ids    评论ID列表
     * @param userId 更新用户ID
     * @return 影响行数
     */
    int batchLogicDeleteComments(@Param("ids") List<Long> ids, @Param("userId") Long userId);


    /**
     * 根据根评论ID列表查询所有回复
     *
     * @param rootIds 根评论ID列表
     * @return 评论回复列表
     */
    List<Comment> selectAllRepliesByRootIds(@Param("rootIds") Collection<Long> rootIds);
}