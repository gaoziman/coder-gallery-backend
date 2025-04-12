package org.leocoder.picture.domain.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-13 00:23
 * @description : 图片哈希表，用于防止重复抓取
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PictureHash {
    private Long id;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * URL的MD5哈希值
     */
    private String urlHash;

    /**
     * 原始URL
     */
    private String url;

    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 抓取源
     */
    private String source;

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