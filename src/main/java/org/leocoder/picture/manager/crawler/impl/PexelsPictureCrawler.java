package org.leocoder.picture.manager.crawler.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 17:05
 * @description : Pexels图片抓取实现
 */
@Slf4j
@Component
public class PexelsPictureCrawler extends BasePictureCrawler {
    
    @Value("${picture.crawler.pexels.api-key:}")
    private String apiKey;
    
    private static final String API_URL = "https://api.pexels.com/v1/search";
    
    @Override
    public String getSourceName() {
        return "pexels";
    }
    
    @Override
    public List<Map<String, Object>> crawlPictures(String searchText, Integer count) {
        if (StrUtil.isBlank(apiKey)) {
            log.error("Pexels API密钥未配置");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Pexels API密钥未配置");
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            HttpResponse response = HttpRequest.get(API_URL)
                    .header("Authorization", apiKey)
                    .form("query", searchText)
                    .form("per_page", count)
                    .execute();
            
            if (!isValidResponse(response)) {
                log.error("Pexels API响应异常: {}", response.getStatus());
                return result;
            }
            
            String body = response.body();
            JSONObject json = JSONUtil.parseObj(body);
            JSONArray photos = json.getJSONArray("photos");
            
            for (int i = 0; i < photos.size(); i++) {
                JSONObject photo = photos.getJSONObject(i);
                JSONObject src = photo.getJSONObject("src");
                String imageUrl = src.getStr("large");
                
                // 获取图片宽高
                int width = photo.getInt("width", 0);
                int height = photo.getInt("height", 0);
                
                // 构建图片元数据
                Map<String, Object> metadata = buildPictureMetadata(
                        imageUrl,
                        width,
                        height,
                        "JPG",
                        photo.getStr("alt", "Pexels图片")
                );
                
                result.add(metadata);
            }
        } catch (Exception e) {
            handleCrawlException(e, getSourceName());
        }
        
        return result;
    }
}