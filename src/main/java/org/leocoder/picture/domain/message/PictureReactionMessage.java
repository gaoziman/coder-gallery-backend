package org.leocoder.picture.domain.message;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-21 22:43
 * @description : 用于在RocketMQ中传递用户对图片的点赞、收藏等操作
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "PictureReactionMessage", description = "用于在RocketMQ中传递用户对图片的点赞、收藏等操作")
public class PictureReactionMessage {

    @ApiModelProperty(value = "图片ID")
    private Long pictureId;

    @ApiModelProperty(value = "操作类型（like、favorite等）")
    private String reactionType;

    @ApiModelProperty(value = "操作类型（add、remove）")
    private String operationType;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "操作时间戳")
    private Long timestamp;
}
