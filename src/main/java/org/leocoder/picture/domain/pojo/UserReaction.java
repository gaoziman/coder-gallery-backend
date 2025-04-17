package org.leocoder.picture.domain.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-16 21:53
 * @description : 通用的点赞/收藏表
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "分类关联表")
public class UserReaction {
    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "目标类型(picture-图片,comment-评论,article-文章等)")
    private String targetType;

    @ApiModelProperty(value = "目标ID")
    private Long targetId;

    @ApiModelProperty(value = "类型(like-点赞,dislike-踩,favorite-收藏,view-查看)")
    private String reactionType;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "创建人ID")
    private Long createUser;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "更新人ID")
    private Long updateUser;

    @ApiModelProperty(value = "是否删除(0-未删除,1-已删除)")
    private Integer isDeleted;
}