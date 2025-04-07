package org.leocoder.picture.exception;

import lombok.Getter;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 13:36
 * @description :
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),

    PARAMS_ERROR(40000, "请求参数错误"),

    NOT_LOGIN_ERROR(40100, "未登录"),

    NO_AUTH_ERROR(40101, "无权限"),

    OPERATION_ERROR(50001, "操作失败"),

    BUSINESS_ERROR(50002, "业务异常"),

    SYSTEM_ERROR(50003, "系统内部异常"),


    ACCOUNT_EXIST(40201, "账号已存在"),


    ACCOUNT_NOT_FOUND(40202, "账号不存在"),

    PASSWORD_ERROR(40203, "用户名或密码错误"),


    ACCOUNT_BANNED(40204, "账号已被禁用"),


    EXIST(40205, "数据已存在"),

    NOT_LOGIN(40206, "未登录");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
