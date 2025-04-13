package org.leocoder.picture.domain.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @date 2025-04-11 00:15
 * @version 1.0
 * @description : 图片实体类
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Picture {
    /**
     * 图片ID
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片URL地址
     */
    private String url;

    /**
     * 缩率图URL
     */
    private String thumbnailUrl;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 图片描述
     */
    private String description;

    /**
     * 图片格式(jpg/png/webp/svg等)
     */
    private String format;

    /**
     * 图片宽度(px)
     */
    private Integer picWidth;

    /**
     * 图片高度(px)
     */
    private Integer picHeight;

    /**
     * 图片高宽比例
     */
    private Double picScale;

    /**
     * 图片大小(字节)
     */
    private Long size;


    /**
     * 图片主色调(HEX格式)
     */
    private String mainColor;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 浏览次数
     */
    private Long viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 收藏数
     */
    private Integer collectionCount;

    /**
     * 审核状态(0--待审核/1-已通过/2-已拒绝)
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人ID
     */
    private Long reviewUserId;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建人ID
     */
    private Long createUser;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 修改人ID
     */
    private Long updateUser;

    /**
     * 是否删除(0-未删除/1-已删除)
     */
    private Integer isDeleted;
}