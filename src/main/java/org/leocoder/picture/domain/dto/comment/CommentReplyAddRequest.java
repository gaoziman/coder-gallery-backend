package org.leocoder.picture.domain.dto.comment;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @date  2025-04-17 22:52
 * @version 1.0
 * @description : 评论回复添加请求
 */
@Data
@ApiModel(value = "CommentReplyAddRequest", description = "评论回复添加请求")
public class CommentReplyAddRequest{
    
    @ApiModelProperty(value = "评论内容", required = true)
    private String content;
    
    @ApiModelProperty(value = "父评论ID", required = true)
    private Long parentId;
    
    @ApiModelProperty(value = "被回复用户ID")
    private Long replyUserId;
    
}