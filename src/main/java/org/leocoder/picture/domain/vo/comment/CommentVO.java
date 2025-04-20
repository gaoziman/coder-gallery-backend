package org.leocoder.picture.domain.vo.comment;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.leocoder.picture.domain.vo.user.UserVO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : 程序员Leo
 * @date  2025-04-17 22:52
 * @version 1.0
 * @description : 评论返回VO
 */
@Data
@ApiModel(value = "CommentVO", description = "评论返回VO")
public class CommentVO {

    @ApiModelProperty(value = "评论ID")
    private Long id;

    @ApiModelProperty(value = "评论内容")
    private String content;

    @ApiModelProperty(value = "父评论ID(一级评论为NULL)")
    private Long parentId;

    @ApiModelProperty(value = "根评论ID(一级评论为NULL)")
    private Long rootId;

    @ApiModelProperty(value = "评论对象类型")
    private String contentType;

    @ApiModelProperty(value = "评论对象ID")
    private Long contentId;

    @ApiModelProperty(value = "回复数量")
    private Integer replyCount;

    @ApiModelProperty(value = "点赞数量")
    private Integer likeCount;

    @ApiModelProperty(value = "状态(pending-待审核,approved-已通过,rejected-已拒绝,reported-被举报)")
    private String status;

    @ApiModelProperty(value = "是否置顶(0-否,1-是)")
    private Boolean isTop;

    @ApiModelProperty(value = "是否热门(0-否,1-是)")
    private Boolean isHot;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "评论者信息")
    private UserVO user;

    @ApiModelProperty(value = "被回复用户信息")
    private UserVO replyUser;

    @ApiModelProperty(value = "回复列表")
    private List<CommentVO> replies;

    @ApiModelProperty(value = "当前用户是否已点赞")
    private Boolean hasLiked;

    @ApiModelProperty(value = "子评论列表")
    private List<CommentVO> children = new ArrayList<>();
}