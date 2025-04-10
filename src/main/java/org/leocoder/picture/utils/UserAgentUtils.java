package org.leocoder.picture.utils;

import cn.hutool.core.util.StrUtil;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;

import javax.servlet.http.HttpServletRequest;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 14:45
 * @description : User-Agent解析工具类
 */
public class UserAgentUtils {
    
    /**
     * 获取浏览器信息
     *
     * @param request 请求对象
     * @return 浏览器信息
     */
    public static String getBrowserInfo(HttpServletRequest request) {
        String userAgentStr = request.getHeader("User-Agent");
        if (StrUtil.isBlank(userAgentStr)) {
            return "未知浏览器";
        }
        
        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentStr);
        Browser browser = userAgent.getBrowser();
        
        return browser.getName() + " " + browser.getVersion(userAgentStr);
    }
    
    /**
     * 获取操作系统信息
     *
     * @param request 请求对象
     * @return 操作系统信息
     */
    public static String getOsInfo(HttpServletRequest request) {
        String userAgentStr = request.getHeader("User-Agent");
        if (StrUtil.isBlank(userAgentStr)) {
            return "未知操作系统";
        }
        
        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentStr);
        OperatingSystem os = userAgent.getOperatingSystem();
        
        return os.getName();
    }
    
    /**
     * 获取设备信息
     *
     * @param request 请求对象
     * @return 设备信息
     */
    public static String getDeviceInfo(HttpServletRequest request) {
        String userAgentStr = request.getHeader("User-Agent");
        if (StrUtil.isBlank(userAgentStr)) {
            return "未知设备";
        }
        
        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentStr);
        OperatingSystem os = userAgent.getOperatingSystem();
        
        if (os.isMobileDevice()) {
            return "移动设备";
        } else {
            return "PC设备";
        }
    }
}