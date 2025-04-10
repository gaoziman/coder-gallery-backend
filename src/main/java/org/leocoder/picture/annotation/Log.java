package org.leocoder.picture.annotation;

import java.lang.annotation.*;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 14:25
 * @description : 操作日志注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 操作类型
     */
    String action() default "";

    /**
     * 是否保存请求参数
     */
    boolean saveParams() default true;

    /**
     * 是否保存响应结果
     */
    boolean saveResult() default false;

    /**
     * 排除敏感字段（如密码等）
     */
    String[] excludeFields() default {};
}