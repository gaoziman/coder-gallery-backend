package org.leocoder.picture.domain.mapstruct;

import org.leocoder.picture.domain.pojo.Comment;
import org.leocoder.picture.domain.vo.comment.CommentVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-18 10:10
 * @description : 评论对象转换器
 */
@Mapper
public interface CommentConvert {
    CommentConvert INSTANCE = Mappers.getMapper(CommentConvert.class);

    /**
     * 评论实体转VO
     */
    CommentVO toCommentVO(Comment comment);

    /**
     * 评论实体列表转VO列表
     */
    List<CommentVO> toCommentVOList(List<Comment> commentList);
}