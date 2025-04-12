package org.leocoder.picture.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.leocoder.picture.annotation.Log;
import org.leocoder.picture.domain.pojo.OperationLog;
import org.leocoder.picture.mapper.OperationLogMapper;
import org.leocoder.picture.utils.IpUtils;
import org.leocoder.picture.utils.SecurityUtils;
import org.leocoder.picture.utils.SensitiveInfoUtils;
import org.leocoder.picture.utils.SnowflakeIdGenerator;
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
 * @date 2025-04-10 14:28
 * @description : 操作日志切面
 */
@Slf4j
@Aspect
@Order(2)
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final OperationLogMapper operationLogMapper;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 配置切入点
     */
    @Pointcut("@annotation(org.leocoder.picture.annotation.Log)")
    public void logPointCut() {
    }

    /**
     * 环绕通知处理
     */
    @Around("logPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 开始时间
        long startTime = System.currentTimeMillis();
        Object result = null;
        
        try {
            // 先执行目标方法
            result = joinPoint.proceed();
            
            // 执行成功，记录日志
            handleLog(joinPoint, result, null, System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            // 执行失败，记录异常日志
            handleLog(joinPoint, null, e, System.currentTimeMillis() - startTime);
            throw e;
        }
    }

    /**
     * 处理操作日志
     *
     * @param joinPoint 切点
     * @param result    结果
     * @param e         异常
     * @param time      耗时(毫秒)
     */
    @Async
    protected void handleLog(JoinPoint joinPoint, Object result, Exception e, long time) {
        try {
            // 获取当前用户ID
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                // 用户未登录，不记录日志
                return;
            }

            // 获取方法签名
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            
            // 获取注解信息
            Log logAnnotation = method.getAnnotation(Log.class);
            if (logAnnotation == null) {
                return;
            }
            
            // 获取API操作名称
            String actionName = logAnnotation.action();
            if (StrUtil.isBlank(actionName)) {
                ApiOperation apiOperation = method.getAnnotation(ApiOperation.class);
                if (apiOperation != null) {
                    actionName = apiOperation.value();
                }
            }
            
            // 构建操作日志对象
            OperationLog operationLog = new OperationLog();
            operationLog.setId(snowflakeIdGenerator.nextId());
            operationLog.setUserId(userId);
            operationLog.setModule(logAnnotation.module());
            operationLog.setAction(actionName);
            operationLog.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());
            operationLog.setTime(time);
            operationLog.setOperationTime(LocalDateTime.now());
            
            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                operationLog.setIp(IpUtils.getIpAddress(request));
            }
            
            // 处理请求参数
            if (logAnnotation.saveParams()) {
                // 过滤敏感字段
                String params = SensitiveInfoUtils.filterSensitiveInfo(
                        JSONUtil.toJsonStr(joinPoint.getArgs()), 
                        logAnnotation.excludeFields()
                );
                operationLog.setParams(params);
            }
            
            // 处理响应结果
            if (logAnnotation.saveResult() && result != null) {
                // 由于响应结果可能很大，限制长度
                String resultStr = JSONUtil.toJsonStr(result);
                if (resultStr.length() > 2000) {
                    resultStr = resultStr.substring(0, 2000) + "...";
                }
                operationLog.setParams(operationLog.getParams() + "\nResponse: " + resultStr);
            }
            
            // 处理异常信息
            if (e != null) {
                operationLog.setStatus(0);
                operationLog.setErrorMsg(StrUtil.sub(e.getMessage(), 0, 2000));
            } else {
                operationLog.setStatus(0);
            }
            
            // 设置基础字段
            operationLog.setCreateTime(LocalDateTime.now());
            operationLog.setCreateBy(userId);
            operationLog.setIsDeleted(0);
            
            // 保存日志
            operationLogMapper.insertWithId(operationLog);
        } catch (Exception ex) {
            // 记录日志过程中的异常不应影响业务
            log.error("记录操作日志异常", ex);
        }
    }
}