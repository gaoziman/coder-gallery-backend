package org.leocoder.picture.manager.crawler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 16:35
 * @description : 图片抓取管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PictureCrawlerManager {

    // 注入所有图片抓取器实现
    private final List<PictureCrawler> pictureCrawlers;
    
    // 抓取器映射，键为抓取源名称
    private Map<String, PictureCrawler> crawlerMap;
    
    @PostConstruct
    public void init() {
        crawlerMap = pictureCrawlers.stream()
                .collect(Collectors.toMap(PictureCrawler::getSourceName, crawler -> crawler));
        log.info("已加载 {} 个图片抓取源: {}", crawlerMap.size(), 
                crawlerMap.keySet().stream().collect(Collectors.joining(", ")));
    }
    
    /**
     * 获取指定抓取源
     * @param source 抓取源名称
     * @return 抓取源实现
     */
    public PictureCrawler getCrawler(String source) {
        if ("all".equalsIgnoreCase(source)) {
            // 随机选择一个抓取源
            List<PictureCrawler> crawlers = new ArrayList<>(crawlerMap.values());
            int randomIndex = new Random().nextInt(crawlers.size());
            PictureCrawler crawler = crawlers.get(randomIndex);
            log.info("随机选择抓取源: {}", crawler.getSourceName());
            return crawler;
        }
        
        // 获取指定抓取源
        PictureCrawler crawler = crawlerMap.get(source.toLowerCase());
        if (crawler == null) {
            log.warn("未找到抓取源: {}, 将使用默认抓取源", source);
            // 使用第一个可用的抓取源作为默认值
            crawler = crawlerMap.values().iterator().next();
        }
        return crawler;
    }
    
    /**
     * 获取所有支持的抓取源名称
     * @return 抓取源名称列表
     */
    public List<String> getSupportedSources() {
        return new ArrayList<>(crawlerMap.keySet());
    }
}