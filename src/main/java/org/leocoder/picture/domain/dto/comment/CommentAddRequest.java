package org.leocoder.picture.domain.dto.comment;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @date  2025-04-17 22:52
 * @version 1.0
 * @description : 评论添加请求
 */
@Data
@ApiModel(value = "CommentAddRequest", description = "评论添加请求")
public class CommentAddRequest {
    
    @ApiModelProperty(value = "评论内容", required = true)
    private String content;
    
    @ApiModelProperty(value = "评论对象类型(article-文章评论,picture-图片评论,product-产品评论等)", required = true)
    private String contentType;
    
    @ApiModelProperty(value = "评论对象ID", required = true)
    private Long contentId;
    
}