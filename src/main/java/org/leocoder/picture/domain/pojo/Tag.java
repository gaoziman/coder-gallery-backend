package org.leocoder.picture.domain.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @date  2025-04-09 10:23
 * @version 1.0
 * @description :
 */

/**
 * 标签表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tag {
    /**
    * 标签ID
    */
    private Long id;

    /**
    * 标签名称
    */
    private String name;

    /**
    * 标签颜色(HEX格式，如#1890FF)
    */
    private String color;

    /**
    * 标签描述
    */
    private String description;

    /**
    * 状态(active-已启用,inactive-未启用)
    */
    private String status;

    /**
    * 引用次数
    */
    private Integer referenceCount;

    /**
    * 排序优先级(数值越小排序越靠前)
    */
    private Integer sortOrder;

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