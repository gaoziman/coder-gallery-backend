package org.leocoder.picture.domain.dto.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.leocoder.picture.common.PageRequest;

@Data
@ApiModel(value = "SimilarPictureRequest", description = "相似图片查询请求")
public class SimilarPictureRequest extends PageRequest {
    
    @ApiModelProperty(value = "图片ID", required = true, example = "1")
    private Long pictureId;
    
    @ApiModelProperty(value = "是否包含同一用户图片", example = "true")
    private Boolean includeSameUser = true;
    
    @ApiModelProperty(value = "匹配类型(1:所有维度 2:仅分类 3:仅标签)", example = "1")
    private Integer matchType = 1;
}