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
 * @description : 目标的点赞/收藏计数（点赞数、收藏数等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "UserReactionCountVO", description = "目标的反应计数")
public class UserReactionCountVO {

    @ApiModelProperty(value = "目标ID")
    private Long targetId;

    @ApiModelProperty(value = "目标类型")
    private String targetType;

    @ApiModelProperty(value = "点赞数")
    private Long likeCount;

    @ApiModelProperty(value = "收藏数")
    private Long favoriteCount;

    @ApiModelProperty(value = "浏览数")
    private Long viewCount;
}