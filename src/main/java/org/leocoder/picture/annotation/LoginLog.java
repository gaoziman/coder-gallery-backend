package org.leocoder.picture.annotation;

import java.lang.annotation.*;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 14:26
 * @description : 登录日志注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoginLog {
    /**
     * 登录类型（登录、登出等）
     */
    String type() default "login";

    /**
     * 登录状态表达式（SpEL表达式，用于动态判断登录状态）
     */
    String statusExp() default "'true'";

    /**
     * 登录消息表达式（SpEL表达式，用于动态生成登录消息）
     */
    String messageExp() default "";
}