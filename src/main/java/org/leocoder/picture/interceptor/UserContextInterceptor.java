package org.leocoder.picture.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.mapper.UserMapper;
import org.leocoder.picture.utils.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 16:50
 * @description : 用户上下文拦截器，用于自动设置当前登录用户到线程上下文
 */
@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断当前是否已登录
        if (StpUtil.isLogin()) {
            // 获取当前登录用户ID
            Long userId = StpUtil.getLoginIdAsLong();
            // 查询用户完整信息
            User user = userMapper.selectById(userId);
            // 设置到当前线程上下文
            if (user != null) {
                UserContext.setUser(user);
            }
        }
        return true;
    }


    /**
     * 请求结束后清除线程上下文数据，防止内存泄漏
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器对象
     * @param ex       异常对象
     * @throws Exception 异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response, Object handler,
                                Exception ex) throws Exception {
        // 请求结束后清除线程上下文数据，防止内存泄漏
        UserContext.clear();
    }
}