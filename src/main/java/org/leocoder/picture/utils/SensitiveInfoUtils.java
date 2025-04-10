package org.leocoder.picture.utils;

import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 14:48
 * @description : 敏感信息过滤工具
 */
public class SensitiveInfoUtils {
    
    /**
     * 默认需要过滤的敏感字段
     */
    private static final String[] DEFAULT_SENSITIVE_FIELDS = {
            "password", "passwd", "secret", "token", "accessToken", "creditCard",
            "cardNo", "idCard", "身份证", "银行卡", "密码", "私钥", "passWord"
    };
    
    /**
     * 过滤敏感信息
     *
     * @param jsonStr      JSON字符串
     * @param excludeFields 需要排除的字段数组
     * @return 过滤后的JSON字符串
     */
    public static String filterSensitiveInfo(String jsonStr, String[] excludeFields) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return jsonStr;
        }
        
        // 合并默认字段和用户指定字段
        Set<String> sensitiveFields = new HashSet<>(Arrays.asList(DEFAULT_SENSITIVE_FIELDS));
        if (ArrayUtil.isNotEmpty(excludeFields)) {
            sensitiveFields.addAll(Arrays.asList(excludeFields));
        }
        
        try {
            // 解析JSON
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            
            // 遍历处理敏感字段
            for (String field : sensitiveFields) {
                if (jsonObject.containsKey(field)) {
                    jsonObject.put(field, "******");
                }
            }
            
            return jsonObject.toJSONString();
        } catch (Exception e) {
            // 如果不是合法的JSON字符串，使用正则表达式替换
            for (String field : sensitiveFields) {
                // 替换 "field":"value" 或 "field": "value" 的模式
                String regex = "\"" + field + "\"\\s*:\\s*\"([^\"]*)\"";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(jsonStr);
                
                if (matcher.find()) {
                    jsonStr = jsonStr.replaceAll(regex, "\"" + field + "\":\"******\"");
                }
            }
            
            return jsonStr;
        }
    }
}