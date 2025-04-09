package org.leocoder.picture.domain.pojo;

import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 13:59
 * @description : 分类关联表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRelation {
    /**
     * 关联ID
     */
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 内容类型(picture-图片,article-文章等)
     */
    private String contentType;

    /**
     * 内容ID(目前指的是图片id)
     */
    private Long contentId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 创建人ID
     */
    private Long createUser;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 更新人ID
     */
    private Long updateUser;

    /**
     * 是否删除(0-未删除,1-已删除)
     */
    private Integer isDeleted;
}