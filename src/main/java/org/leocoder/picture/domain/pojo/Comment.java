package org.leocoder.picture.domain.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static org.leocoder.picture.constant.CommonConstant.DELETED;

/**
 * @author : 程序员Leo
 * @date  2025-04-17 22:52
 * @version 1.0
 * @description :评论实体类
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "评论实体类")
public class Comment {
    /**
    * 评论ID
    */
    @ApiModelProperty(value = "评论ID")
    private Long id;

    /**
    * 评论内容
    */
    @ApiModelProperty(value = "评论内容")
    private String content;

    /**
    * 父评论ID(一级评论为NULL)
    */
    @ApiModelProperty(value = "父评论ID")
    private Long parentId;

    /**
    * 根评论ID(一级评论为NULL)
    */
    @ApiModelProperty(value = "根评论ID")
    private Long rootId;

    /**
    * 评论对象类型(article-文章评论,picture-图片评论,product-产品评论等)
    */
    @ApiModelProperty(value = "评论对象类型")
    private String contentType;

    /**
    * 评论对象ID(目前这里指的是图片)
    */
    @ApiModelProperty(value = "评论对象ID")
    private Long contentId;

    /**
    * 被回复用户ID
    */
    @ApiModelProperty(value = "被回复用户ID")
    private Long replyUserId;

    /**
    * 回复数量
    */
    @ApiModelProperty(value = "回复数量")
    private Integer replyCount;

    /**
    * 点赞数量
    */
    @ApiModelProperty(value = "点赞数量")
    private Integer likeCount;

    /**
    * 状态(pending-待审核,approved-已通过,rejected-已拒绝,reported-被举报)
    */
    @ApiModelProperty(value = "状态")
    private String status;

    /**
    * 是否置顶(0-否,1-是)
    */
    @ApiModelProperty( value = "是否置顶")
    private Boolean isTop;

    /**
    * 是否热门(0-否,1-是)
    */
    @ApiModelProperty(value = "是否热门")
    private Boolean isHot;

    @ApiModelProperty(value = "审核人ID")
    private Long reviewUserId;

    @ApiModelProperty(value = "审核时间")
    private LocalDateTime reviewTime;

    @ApiModelProperty(value = "审核备注")
    private String reviewRemark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "创建人ID")
    private Long createUser;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    /**
    * 更新人ID
    */
    @ApiModelProperty(value = "更新人ID")
    private Long updateUser;

    /**
    * 是否删除(0-未删除,1-已删除)
    */
    @ApiModelProperty(value = "是否删除")
    private Integer isDeleted;


    // 判断是否已删除的辅助方法
    public boolean isDeleted() {
        return DELETED.equals(this.getIsDeleted());
    }
}