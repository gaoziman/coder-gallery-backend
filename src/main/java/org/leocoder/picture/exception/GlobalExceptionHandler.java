package org.leocoder.picture.exception;

import cn.dev33.satoken.exception.NotLoginException;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 13:40
 * @description : 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }


    @ExceptionHandler(NotLoginException.class)
    public Result<?> notLoginExceptionHandler(NotLoginException e) {
        log.error("NotLoginException", e);
        String message;

        // 针对不同类型的未登录异常返回友好提示
        if (e.getType().equals(NotLoginException.NOT_TOKEN)) {
            message = "请先登录";
        } else if (e.getType().equals(NotLoginException.INVALID_TOKEN)) {
            message = "登录状态已失效，请重新登录";
        } else if (e.getType().equals(NotLoginException.TOKEN_TIMEOUT)) {
            message = "登录已过期，请重新登录";
        } else if (e.getType().equals(NotLoginException.BE_REPLACED)) {
            message = "您的账号已在其他设备登录";
        } else if (e.getType().equals(NotLoginException.KICK_OUT)) {
            message = "您已被强制下线";
        } else {
            message = "请先登录系统";
        }

        return ResultUtils.error(ErrorCode.NOT_LOGIN, message);
    }
}
