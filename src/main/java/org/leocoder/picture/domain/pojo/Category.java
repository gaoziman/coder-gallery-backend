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
 * @description : 分类表
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Category {
    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父级分类ID
     */
    private Long parentId;

    /**
     * 分类类型(product-产品分类,article-文章分类等)
     */
    private String type;

    /**
     * 分类层级(1-一级,2-二级,3-三级)
     */
    private Integer level;

    /**
     * 分类路径(格式:1,2,3表示从一级到三级的ID路径)
     */
    private String path;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 分类图标
     */
    private String icon;

    /**
     * 分类别名(用于URL优化)
     */
    private String urlName;

    /**
     * 排序优先级(数值越小排序越靠前)
     */
    private Integer sortOrder;

    /**
     * 包含内容数量
     */
    private Integer contentCount;

    /**
     * 状态(active-已启用,inactive-未启用)
     */
    private String status;

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