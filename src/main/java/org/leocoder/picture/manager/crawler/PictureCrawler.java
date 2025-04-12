package org.leocoder.picture.manager.crawler;

import java.util.List;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 16:30
 * @description : 图片抓取接口
 */
public interface PictureCrawler {
    
    /**
     * 获取抓取源的名称
     * @return 抓取源名称
     */
    String getSourceName();
    
    /**
     * 抓取图片
     * @param searchText 搜索文本
     * @param count 抓取数量
     * @return 抓取到的图片列表，包含URL和元数据
     */
    List<Map<String, Object>> crawlPictures(String searchText, Integer count);
}