package org.leocoder.picture.config;

import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.interceptor.UserContextInterceptor;
import org.leocoder.picture.mapper.UserMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 15:10
 * @description : Web MVC配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    private final UserMapper userMapper;

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册用户上下文拦截器，拦截所有请求
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/**");

        // 注册Sa-Token拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 登录验证 -- 拦截所有需要登录的接口
            SaRouter.match("/**")
                    // 排除登录注册接口 - 使用通配符匹配，确保能匹配到带有context-path的路径
                    .notMatch("/**/user/login", "/**/user/register")
                    // 排除静态资源和Swagger相关接口
                    .notMatch("/doc.html", "/webjars/**", "/swagger-resources/**", "/v3/api-docs/**")
                    // 检查是否登录
                    .check(r -> StpUtil.checkLogin());

            // 角色验证 -- 拦截管理员专属接口
            SaRouter.match("/**/admin/**")
                    .check(r -> {
                        // 获取用户ID
                        Long userId = StpUtil.getLoginIdAsLong();
                        // 直接从数据库查询用户角色
                        User user = userMapper.selectById(userId);
                        // 检查是否为admin角色
                        if (user == null || !"admin".equals(user.getRole())) {
                            throw new NotRoleException("admin");
                        }
                    });

        })).addPathPatterns("/**");
    }
}