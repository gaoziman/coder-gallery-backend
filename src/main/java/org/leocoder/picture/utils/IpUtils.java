package org.leocoder.picture.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 14:58
 * @description : IP工具类
 */
@Slf4j
public class IpUtils {

    private static final String UNKNOWN = "unknown";
    private static final String LOCAL_IP = "127.0.0.1";
    private static final String LOCAL_IPV6 = "0:0:0:0:0:0:0:1";
    private static final int IP_LEN = 15;

    /**
     * 获取客户端IP
     *
     * @param request 请求对象
     * @return IP地址
     */
    public static String getIpAddress(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ip = request.getHeader("x-forwarded-for");
        if (isEmptyIP(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isEmptyIP(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isEmptyIP(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isEmptyIP(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isEmptyIP(ip)) {
            ip = request.getRemoteAddr();
            if (LOCAL_IP.equals(ip) || LOCAL_IPV6.equals(ip)) {
                // 根据网卡取本机配置的IP
                try {
                    InetAddress inet = InetAddress.getLocalHost();
                    ip = inet.getHostAddress();
                } catch (UnknownHostException e) {
                    log.error("获取IP地址异常：", e);
                }
            }
        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP，多个IP按照','分割
        if (ip != null && ip.length() > IP_LEN) {
            if (ip.indexOf(",") > 0) {
                ip = ip.substring(0, ip.indexOf(","));
            }
        }

        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    /**
     * 判断IP是否为空
     */
    private static boolean isEmptyIP(String ip) {
        return StrUtil.isEmpty(ip) || UNKNOWN.equalsIgnoreCase(ip);
    }

    /**
     * 判断是否为内网IP
     *
     * @param ip IP地址
     * @return 是否内网IP
     */
    public static boolean internalIp(String ip) {
        if (LOCAL_IP.equals(ip) || LOCAL_IPV6.equals(ip)) {
            return true;
        }

        byte[] addr = textToNumericFormatV4(ip);
        if (addr == null) {
            return false;
        }

        // 10.x.x.x/8
        final byte b0 = addr[0];
        // 172.16.x.x/12
        final byte b1 = addr[1];
        // 192.168.x.x/16
        return b0 == 10 ||
                (b0 == 172 && (b1 >= 16 && b1 <= 31)) ||
                (b0 == 192 && b1 == 168);
    }

    /**
     * 将IPv4地址转换成字节
     *
     * @param text IPv4地址
     * @return byte 字节
     */
    public static byte[] textToNumericFormatV4(String text) {
        if (text.length() == 0) {
            return null;
        }

        byte[] bytes = new byte[4];
        String[] elements = text.split("\\.", -1);
        try {
            if (elements.length != 4) {
                return null;
            }
            for (int i = 0; i < elements.length; i++) {
                int value = Integer.parseInt(elements[i]);
                if (value < 0 || value > 255) {
                    return null;
                }
                bytes[i] = (byte) (value & 0xff);
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return bytes;
    }
}