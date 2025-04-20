package org.leocoder.picture.domain.dto.comment;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.leocoder.picture.common.PageRequest;

import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-18 10:00
 * @description : 管理员评论查询请求参数
 */
@Data
@ApiModel(value = "AdminCommentQueryRequest", description = "管理员评论查询请求参数")
public class AdminCommentQueryRequest extends PageRequest {

    @ApiModelProperty(value = "评论内容(模糊匹配)")
    private String content;

    @ApiModelProperty(value = "评论对象类型(article-文章评论,picture-图片评论,product-产品评论等)")
    private String contentType;

    @ApiModelProperty(value = "评论对象ID")
    private Long contentId;

    @ApiModelProperty(value = "评论用户ID")
    private Long userId;

    @ApiModelProperty(value = "状态(pending-待审核,approved-已通过,rejected-已拒绝,reported-被举报)")
    private String status;

    @ApiModelProperty(value = "是否置顶(0-否,1-是)")
    private Boolean isTop;

    @ApiModelProperty(value = "是否热门(0-否,1-是)")
    private Boolean isHot;

    @ApiModelProperty(value = "仅查看一级评论")
    private Boolean onlyRoot;

    @ApiModelProperty(value = "创建时间起始")
    private LocalDateTime createTimeStart;

    @ApiModelProperty(value = "创建时间截止")
    private LocalDateTime createTimeEnd;
}