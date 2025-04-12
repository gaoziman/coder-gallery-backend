package org.leocoder.picture.manager.crawler.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 17:10
 * @description : 谷歌图片抓取实现 (API版本)
 */
@Slf4j
@Component
public class GooglePictureCrawler extends BasePictureCrawler {

    @Value("${picture.crawler.google.api-key}")
    private String apiKey;

    @Value("${picture.crawler.google.search-engine-id:017576662512468239146:omuauf_lfve}")
    private String searchEngineId;

    // Google Custom Search API URL
    private static final String API_URL = "https://www.googleapis.com/customsearch/v1";

    @Override
    public String getSourceName() {
        return "google";
    }

    @Override
    public List<Map<String, Object>> crawlPictures(String searchText, Integer count) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 验证配置
        if (StrUtil.isBlank(apiKey)) {
            log.error("Google API密钥未配置");
            // 返回空列表而不是抛出异常，让调用者能够降级到其他图片源
            return result;
        }

        if (StrUtil.isBlank(searchEngineId)) {
            log.error("Google 搜索引擎ID未配置");
            return result;
        }

        try {
            // 计算需要发起的请求次数
            // Google API一次最多返回10个结果
            int totalRequests = (count + 9) / 10; // 向上取整
            int itemsLeft = count;

            for (int requestNum = 0; requestNum < totalRequests && itemsLeft > 0; requestNum++) {
                // 当前请求的起始索引
                int startIndex = requestNum * 10 + 1; // Google API索引从1开始

                // 当前请求需要的结果数量
                int currentBatchSize = Math.min(itemsLeft, 10);

                // 构建API URL
                String url = API_URL +
                        "?key=" + apiKey +
                        "&cx=" + searchEngineId +
                        "&q=" + URLEncoder.encode(searchText, StandardCharsets.UTF_8) +
                        "&searchType=image" +
                        "&num=" + currentBatchSize +
                        "&start=" + startIndex +
                        "&safe=off"; // 可选参数，控制安全搜索级别

                log.debug("发送Google Custom Search API请求: {}", url);

                // 执行HTTP请求
                HttpResponse response = HttpRequest.get(url)
                        .timeout(15000) // 增加超时时间
                        .execute();

                // 检查响应是否成功
                if (!isValidResponse(response)) {
                    log.error("Google API响应失败: HTTP {}", response.getStatus());
                    // 尝试解析错误信息
                    try {
                        JSONObject errorJson = JSONUtil.parseObj(response.body());
                        if (errorJson.containsKey("error")) {
                            JSONObject error = errorJson.getJSONObject("error");
                            log.error("Google API错误: {} - {}",
                                    error.getStr("code", "未知错误代码"),
                                    error.getStr("message", "未知错误信息"));
                        }
                    } catch (Exception e) {
                        log.error("无法解析Google API错误响应");
                    }
                    break; // 结束请求循环
                }

                // 解析JSON响应
                JSONObject responseJson = JSONUtil.parseObj(response.body());

                // 检查是否有搜索结果
                if (!responseJson.containsKey("items")) {
                    log.warn("Google API响应中没有搜索结果");
                    break; // 没有更多结果，结束循环
                }

                // 获取搜索结果列表
                JSONArray items = responseJson.getJSONArray("items");
                log.info("获取到 {} 个Google图片搜索结果", items.size());

                // 处理结果
                for (int i = 0; i < items.size() && result.size() < count; i++) {
                    JSONObject item = items.getJSONObject(i);

                    try {
                        String imageUrl = item.getStr("link");

                        // 获取图片尺寸
                        JSONObject image = item.getJSONObject("image");
                        int width = image.getInt("width", 0);
                        int height = image.getInt("height", 0);

                        // 获取图片标题
                        String title = item.getStr("title", searchText + "图片");

                        // 构建图片元数据
                        Map<String, Object> metadata = buildPictureMetadata(
                                imageUrl,
                                width,
                                height,
                                extractFormatFromUrl(imageUrl),
                                title
                        );

                        // 添加来源网站信息
                        if (item.containsKey("displayLink")) {
                            metadata.put("source", item.getStr("displayLink"));
                        }

                        result.add(metadata);
                        log.debug("处理Google图片: {}", imageUrl);
                    } catch (Exception e) {
                        log.warn("处理Google搜索结果项时出错: {}", e.getMessage());
                        // 继续处理下一项
                    }
                }

                // 更新剩余需要获取的结果数量
                itemsLeft -= items.size();

                // 如果已经获取到足够的图片，就退出循环
                if (result.size() >= count) {
                    break;
                }

                // 在多次请求之间添加延迟，避免触发限流
                if (requestNum < totalRequests - 1 && itemsLeft > 0) {
                    Thread.sleep(500);
                }
            }

            log.info("成功从Google获取 {} 张图片", result.size());

        } catch (Exception e) {
            log.error("从Google获取图片时发生错误", e);
        }

        return result;
    }
}