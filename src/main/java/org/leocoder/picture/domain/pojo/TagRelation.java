package org.leocoder.picture.domain.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @date  2025-04-09 10:24
 * @version 1.0
 * @description : 标签关联表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TagRelation {
    /**
    * 关联ID
    */
    private Long id;

    /**
    * 标签ID
    */
    private Long tagId;

    /**
    * 内容类型(picture-图片,article-文章等)
    */
    private String contentType;

    /**
    * 内容ID
    */
    private Long contentId;

    /**
    * 创建时间
    */
    private LocalDateTime createTime;

    /**
    * 创建人ID
    */
    private Long createUser;

    /**
    * 更新时间
    */
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