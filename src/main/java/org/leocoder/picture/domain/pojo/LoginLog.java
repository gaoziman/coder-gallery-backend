package org.leocoder.picture.domain.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 13:37
 * @description : 用户登录记录实体类
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginLog {
    /**
     * 记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 登出时间
     */
    private LocalDateTime logoutTime;

    /**
     * 登录IP
     */
    private String ip;

    /**
     * 登录地点
     */
    private String location;

    /**
     * 登录设备
     */
    private String device;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 登录状态(1-失败,0-成功)
     */
    private Integer status;

    /**
     * 登录消息
     */
    private String message;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 修改人ID
     */
    private Long updateBy;

    /**
     * 是否删除(0-未删除,1-已删除)
     */
    private Integer isDeleted;
}