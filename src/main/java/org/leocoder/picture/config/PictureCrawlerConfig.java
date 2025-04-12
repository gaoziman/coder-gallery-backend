package org.leocoder.picture.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 17:20
 * @description : 图片抓取配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "picture.crawler")
public class PictureCrawlerConfig {
    
    /**
     * Unsplash配置
     */
    private ApiConfig unsplash = new ApiConfig();
    
    /**
     * Pixabay配置
     */
    private ApiConfig pixabay = new ApiConfig();
    
    /**
     * Pexels配置
     */
    private ApiConfig pexels = new ApiConfig();
    
    @Data
    public static class ApiConfig {
        /**
         * API密钥
         */
        private String apiKey;
    }
}