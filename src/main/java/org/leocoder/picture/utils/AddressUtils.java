package org.leocoder.picture.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 14:55
 * @description : IP地址查询工具
 */
@Slf4j
public class AddressUtils {
    
    /**
     * IP地址查询API
     */
    private static final String IP_URL = "https://whois.pconline.com.cn/ipJson.jsp";
    
    /**
     * 未知地址
     */
    private static final String UNKNOWN = "未知";
    
    /**
     * 根据IP地址获取所在地
     *
     * @param ip IP地址
     * @return 所在地
     */
    public static String getRealAddressByIP(String ip) {
        if (StrUtil.isBlank(ip)) {
            return UNKNOWN;
        }
        
        // 内网IP直接返回内网
        if (IpUtils.internalIp(ip)) {
            return "内网IP";
        }
        
        try {
            String rspStr = HttpUtil.get(IP_URL + "?ip=" + ip + "&json=true", 3000);
            if (StrUtil.isEmpty(rspStr)) {
                log.error("获取地理位置异常：{}", ip);
                return UNKNOWN;
            }
            
            JSONObject obj = JSONObject.parseObject(rspStr);
            String region = obj.getString("pro");
            String city = obj.getString("city");
            
            return String.format("%s %s", region, city);
        } catch (Exception e) {
            log.error("获取地理位置异常：{}", ip, e);
        }
        
        return UNKNOWN;
    }
}