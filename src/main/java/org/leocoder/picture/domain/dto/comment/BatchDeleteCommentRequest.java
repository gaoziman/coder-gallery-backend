package org.leocoder.picture.domain.dto.comment;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-18 10:05
 * @description : 批量删除评论请求
 */
@Data
@ApiModel(value = "BatchDeleteCommentRequest", description = "批量删除评论请求")
public class BatchDeleteCommentRequest {

    @ApiModelProperty(value = "评论ID列表", required = true)
    private List<Long> ids;
}