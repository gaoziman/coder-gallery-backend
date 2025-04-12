package org.leocoder.picture.exception;

import lombok.Getter;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 13:36
 * @description : 系统错误码
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

    NOT_FOUND_ERROR(404,  "资源不存在"),


    ACCOUNT_EXIST(40201, "账号已存在"),


    ACCOUNT_NOT_FOUND(40202, "账号不存在"),

    PASSWORD_ERROR(40203, "用户名或密码错误"),


    ACCOUNT_BANNED(40204, "账号已被禁用"),


    NOT_LOGIN(40206, "未登录"),

    NO_AUTH(40207, "无权限"),

    // 数据相关错误码
    DATA_EXIST(40301, "数据已存在"),

    DATA_NOT_FOUND(40302, "数据不存在"),

    DATA_USED(40303, "数据已被使用"),

    // 分类相关错误码
    CATEGORY_NAME_EXIST(40401, "分类名称已存在"),

    CATEGORY_NOT_FOUND(40402, "分类不存在"),

    CATEGORY_HAS_CHILDREN(40403, "分类下有子分类"),

    CATEGORY_HAS_CONTENT(40404, "分类下有关联内容"),

    CATEGORY_CANNOT_MOVE(40405, "分类无法移动到目标位置"),

    CATEGORY_PATH_ERROR(40406, "分类路径错误"),

    // 文件相关错误码
    FILE_UPLOAD_ERROR(40501, "文件上传失败"),

    FILE_DOWNLOAD_ERROR(40502, "文件下载失败"),

    FILE_TYPE_ERROR(40503, "文件类型错误"),

    FILE_SIZE_ERROR(40504, "文件大小不能超过6M"),

    FILE_NOT_NULL(40505, "文件不能为空"),

    // 标签相关错误码
    TAG_NAME_EXIST( 40601, "标签名称已存在" ),

    TAG_NOT_FOUND( 40602, "标签不存在" ),

    TAG_HAS_CONTENT( 40603, "标签下有关联内容" ),

    TAG_CANNOT_MOVE( 40604, "标签无法移动到目标位置" ),

    TAG_PATH_ERROR( 40605, "标签路径错误" ),

    TAG_NAME_ALREADY_EXISTS( 40606, "标签名称已存在" ),


;

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