package org.leocoder.picture.aspect;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.domain.dto.user.UserLoginRequest;
import org.leocoder.picture.domain.pojo.LoginLog;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.user.LoginUserVO;
import org.leocoder.picture.mapper.LoginLogMapper;
import org.leocoder.picture.mapper.UserMapper;
import org.leocoder.picture.utils.AddressUtils;
import org.leocoder.picture.utils.IpUtils;
import org.leocoder.picture.utils.SnowflakeIdGenerator;
import org.leocoder.picture.utils.UserAgentUtils;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 14:30
 * @description : 登录日志切面
 */
@Slf4j
@Aspect
@Order(1)
@Component
@RequiredArgsConstructor
public class LoginLogAspect {

    private final LoginLogMapper loginLogMapper;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private final UserMapper userMapper;

    /**
     * 配置切入点
     */
    @Pointcut("@annotation(org.leocoder.picture.annotation.LoginLog)")
    public void loginLogPointCut() {
    }

    /**
     * 登录成功后处理
     */
    @AfterReturning(pointcut = "loginLogPointCut()", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        try {
            // 获取方法签名
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            // 获取注解信息
            org.leocoder.picture.annotation.LoginLog loginLogAnnotation = method.getAnnotation(org.leocoder.picture.annotation.LoginLog.class);
            if (loginLogAnnotation == null) {
                return;
            }

            // 根据登录类型处理
            String type = loginLogAnnotation.type();

            if ("login".equals(type)) {
                // 处理登录操作
                handleLoginLog(joinPoint, result, 0, null);
            } else if ("logout".equals(type)) {
                // 处理登出操作
                handleLogoutLog(joinPoint, result, 0, null);
            }
        } catch (Exception e) {
            // 记录日志过程中的异常不应影响业务
            log.error("处理登录/登出日志异常", e);
        }
    }

    /**
     * 登录/登出异常后处理
     */
    @AfterThrowing(pointcut = "loginLogPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        try {
            // 获取方法签名
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            // 获取注解信息
            org.leocoder.picture.annotation.LoginLog loginLogAnnotation = method.getAnnotation(org.leocoder.picture.annotation.LoginLog.class);
            if (loginLogAnnotation == null) {
                return;
            }

            // 根据登录类型处理
            String type = loginLogAnnotation.type();

            if ("login".equals(type)) {
                // 处理登录失败
                handleLoginLog(joinPoint, null, 1, e.getMessage());
            } else if ("logout".equals(type)) {
                // 处理登出失败
                handleLogoutLog(joinPoint, null, 1, e.getMessage());
            }
        } catch (Exception ex) {
            // 记录日志过程中的异常不应影响业务
            log.error("处理异常日志失败", ex);
        }
    }


    /**
     * 处理登录日志
     *
     * @param joinPoint 切点
     * @param result    返回结果
     * @param status    状态（成功/失败）
     * @param errorMsg  错误信息
     */
    @Async
    protected void handleLoginLog(JoinPoint joinPoint, Object result, Integer status, String errorMsg) {
        try {
            // 获取请求参数
            Object[] args = joinPoint.getArgs();
            Long userId = null;
            String account = null;

            // 从请求参数中获取账号
            if (args.length > 0 && args[0] instanceof UserLoginRequest) {
                UserLoginRequest loginRequest = (UserLoginRequest) args[0];
                account = loginRequest.getAccount();
            }

            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                log.warn("无法获取请求属性，登录日志记录失败");
                return;
            }

            HttpServletRequest request = attributes.getRequest();
            String ip = IpUtils.getIpAddress(request);

            // 创建日志对象，先设置基本信息
            LoginLog loginLog = new LoginLog();
            loginLog.setId(snowflakeIdGenerator.nextId());
            loginLog.setLoginTime(LocalDateTime.now());
            loginLog.setIp(ip);
            loginLog.setLocation(AddressUtils.getRealAddressByIP(ip));
            loginLog.setDevice(UserAgentUtils.getDeviceInfo(request));
            loginLog.setBrowser(UserAgentUtils.getBrowserInfo(request));
            loginLog.setOs(UserAgentUtils.getOsInfo(request));
            loginLog.setStatus(status);
            loginLog.setCreateTime(LocalDateTime.now());
            loginLog.setIsDeleted(0);

            // 处理状态和消息
            if (status == 0) {
                // 登录成功的情况
                if (result instanceof Result<?>) {
                    Object data = ((Result<?>) result).getData();
                    if (data instanceof LoginUserVO) {
                        LoginUserVO loginUserVO = (LoginUserVO) data;
                        userId = loginUserVO.getId();
                        loginLog.setUserId(userId);
                        loginLog.setCreateBy(userId);
                        loginLog.setMessage("登录成功");
                    }
                }
            } else {
                // 登录失败的情况
                loginLog.setMessage(StrUtil.isBlank(errorMsg) ? "登录失败" : errorMsg);

                // 尝试通过账号查找用户ID
                if (StrUtil.isNotBlank(account)) {
                    try {
                        // 查询用户信息
                        User user = userMapper.selectByAccount(account);
                        if (user != null) {
                            userId = user.getId();
                            loginLog.setUserId(userId);
                            loginLog.setCreateBy(userId);
                        } else {
                            // 账号不存在的情况，记录特殊标记
                            loginLog.setMessage("账号不存在: " + account);
                        }
                    } catch (Exception e) {
                        log.error("查询用户异常", e);
                    }
                }

                // 如果仍然无法确定用户ID（可能是账号不存在）
                if (userId == null) {
                    // 生成一个临时ID，或者设置为0，标记为未知用户
                    loginLog.setUserId(0L);
                    loginLog.setCreateBy(0L);
                }
            }

            // 保存日志
            loginLogMapper.insertWithId(loginLog);

        } catch (Exception e) {
            // 记录日志过程中的异常不应影响业务
            log.error("记录登录日志异常", e);
        }
    }

    /**
     * 处理登出日志
     *
     * @param joinPoint 切点
     * @param result    返回结果
     * @param status    状态（成功/失败）
     * @param errorMsg  错误信息
     */
    @Async
    protected void handleLogoutLog(JoinPoint joinPoint, Object result, Integer status, String errorMsg) {
        try {
            // 获取用户ID（在切面处理时，如果是成功操作，用户已登出，需要先获取）
            Long userId = null;

            // 1. 尝试从线程上下文获取
            if (StpUtil.isLogin()) {
                userId = StpUtil.getLoginIdAsLong();
            }

            // 2. 如果获取不到，可能是已登出，尝试从用户上下文或请求属性获取
            if (userId == null) {
                // 根据项目实际情况，可以从请求或上下文中获取用户ID
                // 示例：从请求属性中获取
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String userIdStr = (String) request.getAttribute("userId");
                    if (StrUtil.isNotBlank(userIdStr)) {
                        try {
                            userId = Long.parseLong(userIdStr);
                        } catch (NumberFormatException e) {
                            log.error("用户ID转换异常", e);
                        }
                    }
                }
            }

            // 如果仍然获取不到用户ID，查找最新的登录记录
            if (userId == null) {
                // 这需要在LoginLogMapper中添加方法，通过IP等信息查找最近的登录记录
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String ip = IpUtils.getIpAddress(request);
                    // 通过IP查找最近的登录记录
                    LoginLog latestLog = loginLogMapper.findLatestLoginByIp(ip);
                    if (latestLog != null) {
                        userId = latestLog.getUserId();
                    }
                }
            }

            // 如果无法获取用户ID，则不记录日志
            if (userId == null) {
                log.warn("无法获取用户ID，登出日志记录失败");
                return;
            }

            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();
            String ip = IpUtils.getIpAddress(request);

            // 查找该用户最近的未登出记录，记录完整的会话
            LoginLog latestLoginLog = loginLogMapper.findLatestLoginWithoutLogout(userId);

            if (latestLoginLog != null) {
                // 如果找到未登出的记录，更新该记录
                latestLoginLog.setLogoutTime(LocalDateTime.now());
                latestLoginLog.setUpdateTime(LocalDateTime.now());
                latestLoginLog.setUpdateBy(userId);
                loginLogMapper.updateByPrimaryKeySelective(latestLoginLog);
            }

            // 构建新的登出日志对象 - 完整记录登出情况
            LoginLog logoutLog = new LoginLog();
            logoutLog.setUserId(snowflakeIdGenerator.nextId());
            logoutLog.setUserId(userId);
            // 如果有最近登录记录，使用其登录时间
            if (latestLoginLog != null) {
                logoutLog.setLoginTime(latestLoginLog.getLoginTime());
            }
            logoutLog.setLogoutTime(LocalDateTime.now());
            logoutLog.setIp(ip);
            logoutLog.setLocation(AddressUtils.getRealAddressByIP(ip));
            logoutLog.setDevice(UserAgentUtils.getDeviceInfo(request));
            logoutLog.setBrowser(UserAgentUtils.getBrowserInfo(request));
            logoutLog.setOs(UserAgentUtils.getOsInfo(request));
            logoutLog.setStatus(status);
            logoutLog.setMessage(status == 0 ? "注销成功" : (StrUtil.isBlank(errorMsg) ? "注销失败" : errorMsg));

            // 设置基础字段
            logoutLog.setCreateTime(LocalDateTime.now());
            logoutLog.setCreateBy(userId);
            logoutLog.setIsDeleted(0);

            // 保存日志
            loginLogMapper.insertWithId(logoutLog);

        } catch (Exception e) {
            // 记录日志过程中的异常不应影响业务
            log.error("记录登出日志异常", e);
        }
    }
}