package org.leocoder.picture.manager.crawler.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.manager.crawler.PictureCrawler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 16:40
 * @description : 图片抓取基类
 */
@Slf4j
public abstract class BasePictureCrawler implements PictureCrawler {
    
    /**
     * 检查响应是否有效
     * @param response HTTP响应
     * @return 是否有效
     */
    protected boolean isValidResponse(HttpResponse response) {
        return response != null && response.isOk();
    }
    
    /**
     * 构建图片元数据
     * @param url 图片URL
     * @param width 宽度
     * @param height 高度
     * @param format 格式
     * @param title 标题
     * @return 图片元数据
     */
    protected Map<String, Object> buildPictureMetadata(String url, Integer width, Integer height, 
                                                      String format, String title) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("url", url);
        metadata.put("thumbnailUrl", url);
        metadata.put("width", width != null ? width : 0);
        metadata.put("height", height != null ? height : 0);
        metadata.put("format", StrUtil.isNotBlank(format) ? format : extractFormatFromUrl(url));
        metadata.put("title", StrUtil.isNotBlank(title) ? title : "抓取图片");
        
        // 计算图片比例
        if (width != null && height != null && height > 0) {
            metadata.put("scale", NumberUtil.round(width * 1.0 / height, 2).doubleValue());
        } else {
            metadata.put("scale", 1.0);
        }
        
        return metadata;
    }
    
    /**
     * 从URL中提取图片格式
     * @param url 图片URL
     * @return 图片格式
     */
    protected String extractFormatFromUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return "jpg";
        }
        
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            return "JPG";
        } else if (lowerUrl.endsWith(".png")) {
            return "PNG";
        } else if (lowerUrl.endsWith(".gif")) {
            return "GIF";
        } else if (lowerUrl.endsWith(".webp")) {
            return "WEBP";
        } else if (lowerUrl.endsWith(".bmp")) {
            return "BMP";
        } else {
            return "JPG";
        }
    }
    
    /**
     * 错误处理
     * @param e 异常
     * @param source 抓取源名称
     */
    protected void handleCrawlException(Exception e, String source) {
        log.error("从 {} 抓取图片时发生错误: {}", source, e.getMessage(), e);
    }
}