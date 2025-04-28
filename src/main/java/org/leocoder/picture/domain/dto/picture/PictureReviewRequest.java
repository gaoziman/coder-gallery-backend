package org.leocoder.picture.domain.dto.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-25 08:45
 * @description : 图片审核请求
 */
@Data
@ApiModel(value = "PictureReviewRequest", description = "图片审核请求")
public class PictureReviewRequest {

    @ApiModelProperty(value = "图片ID")
    private Long pictureId;

    @ApiModelProperty(value = "图片ID列表(批量审核用)")
    private List<Long> pictureIds;

    @ApiModelProperty(value = "审核状态(0-拒绝,1-通过)")
    private Integer reviewStatus;

    @ApiModelProperty(value = "审核意见")
    private String reviewMessage;
}