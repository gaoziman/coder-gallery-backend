package org.leocoder.picture.domain.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author : 程序员Leo
 * @date  2025-04-10 13:38
 * @version 1.0
 * @description : 用户操作日志表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OperationLog {
    /**
    * 日志ID
    */
    private Long id;

    /**
    * 用户ID
    */
    private Long userId;

    /**
    * 操作模块
    */
    private String module;

    /**
    * 操作类型
    */
    private String action;

    /**
    * 请求方法
    */
    private String method;

    /**
    * 请求参数
    */
    private String params;

    /**
    * 执行时长(毫秒)
    */
    private Long time;

    /**
    * 操作IP
    */
    private String ip;

    /**
    * 操作时间
    */
    private Date operationTime;

    /**
    * 操作状态(0-失败,1-成功)
    */
    private Boolean status;

    /**
    * 错误消息
    */
    private String errorMsg;

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