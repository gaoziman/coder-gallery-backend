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
 * @date 2025-04-13 00:23
 * @description : 图片哈希实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "图片哈希实体类")
public class PictureHash {

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "图片ID")
    private Long pictureId;

    @ApiModelProperty(value = "URL的MD5哈希值")
    private String urlHash;

    @ApiModelProperty(value = "原始URL")
    private String url;

    @ApiModelProperty(value = "搜索关键词")
    private String searchText;

    @ApiModelProperty(value = "抓取源")
    private String source;

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