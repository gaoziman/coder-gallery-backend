package org.leocoder.picture.domain.vo.userreaction;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-16 22:25
 * @description : 用户对目标的点赞、收藏、喜欢等状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "UserReactionStatusVO", description = "用户对目标的点赞、收藏等状态")
public class UserReactionStatusVO {

    @ApiModelProperty(value = "目标ID")
    private Long targetId;

    @ApiModelProperty(value = "目标类型")
    private String targetType;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "是否已点赞")
    private boolean hasLiked;

    @ApiModelProperty(value = "是否已收藏")
    private boolean hasFavorited;
}