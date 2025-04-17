package org.leocoder.picture.domain.userreaction;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-16 22:15
 * @description : 用户点赞、踩、收藏、查看等操作请求对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "UserReactionRequest", description = "用户点赞、踩、收藏、查看等操作请求对象")
public class UserReactionRequest {

    @ApiModelProperty(value = "目标类型(picture-图片,comment-评论,article-文章等)")
    private String targetType;

    @ApiModelProperty(value = "目标ID")
    private Long targetId;

    @ApiModelProperty(value = "点赞/收藏类型(like-点赞,dislike-踩,favorite-收藏,view-查看)")
    private String reactionType;
}
