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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 16:45
 * @description : Unsplash图片抓取实现（支持中文关键词翻译）
 */
@Slf4j
@Component
public class UnsplashPictureCrawler extends BasePictureCrawler {

    @Value("${picture.crawler.unsplash.api-key:}")
    private String apiKey;

    // 百度翻译API配置
    @Value("${picture.crawler.baidu.translate.appid:}")
    private String translateAppId;

    @Value("${picture.crawler.baidu.translate.secret:}")
    private String translateSecret;

    private static final String API_URL = "https://api.unsplash.com/search/photos";

    // 百度翻译API
    private static final String BAIDU_TRANSLATE_API = "https://fanyi-api.baidu.com/api/trans/vip/translate";

    // 翻译结果缓存
    private static final Map<String, String> TRANSLATION_CACHE = new ConcurrentHashMap<>();

    // 备用图片URLs（按类别）- 当API调用失败或结果不足时使用
    private static final Map<String, String> FALLBACK_IMAGES = new HashMap<>();

    static {
        // 初始化常用类别的备用图片
        FALLBACK_IMAGES.put("nature", "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?ixlib=rb-4.0.3&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max");
        FALLBACK_IMAGES.put("landscape", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?ixlib=rb-4.0.3&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max");
        FALLBACK_IMAGES.put("people", "https://images.unsplash.com/photo-1517841905240-472988babdf9?ixlib=rb-4.0.3&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max");
        FALLBACK_IMAGES.put("animal", "https://images.unsplash.com/photo-1474511320723-9a56873867b5?ixlib=rb-4.0.3&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max");
        FALLBACK_IMAGES.put("dog", "https://images.unsplash.com/photo-1587300003388-59208cc962cb?ixlib=rb-4.0.3&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max");
        FALLBACK_IMAGES.put("cat", "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?ixlib=rb-4.0.3&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max");
        FALLBACK_IMAGES.put("food", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?ixlib=rb-4.0.3&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max");
        FALLBACK_IMAGES.put("architecture", "https://images.unsplash.com/photo-1487958449943-2429e8be8625?ixlib=rb-4.0.3&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max");
        FALLBACK_IMAGES.put("technology", "https://images.unsplash.com/photo-1518770660439-4636190af475?ixlib=rb-4.0.3&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max");
        FALLBACK_IMAGES.put("default", "https://images.unsplash.com/photo-1553095066-5014bc7b7f2d?ixlib=rb-4.0.3&q=80&fm=jpg&crop=entropy&cs=tinysrgb&w=1080&fit=max");
    }

    @Override
    public String getSourceName() {
        return "unsplash";
    }

    @Override
    public List<Map<String, Object>> crawlPictures(String searchText, Integer count) {
        if (StrUtil.isBlank(apiKey)) {
            log.error("Unsplash API密钥未配置");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Unsplash API密钥未配置");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        String originalSearchText = searchText;

        try {
            // 步骤1: 如果是中文关键词，翻译成英文
            String translatedSearchText = translateToEnglish(searchText);

            if (!searchText.equals(translatedSearchText)) {
                log.info("将搜索关键词从 '{}' 翻译为 '{}'", searchText, translatedSearchText);
                searchText = translatedSearchText;
            }

            log.info("开始从Unsplash搜索 '{}' 相关图片，数量: {}", searchText, count);

            // 步骤2: 调用Unsplash API搜索图片
            HttpResponse response = HttpRequest.get(API_URL)
                    .header("Authorization", "Client-ID " + apiKey)
                    .form("query", searchText)
                    .form("per_page", count)
                    .execute();

            if (!isValidResponse(response)) {
                log.error("Unsplash API响应异常: {}", response.getStatus());
                return ensureImageCount(result, originalSearchText, count);
            }

            String body = response.body();
            JSONObject json = JSONUtil.parseObj(body);
            JSONArray results = json.getJSONArray("results");

            log.info("从Unsplash获取到 {} 个图片搜索结果", results.size());

            // 步骤3: 处理搜索结果
            for (int i = 0; i < results.size(); i++) {
                JSONObject item = results.getJSONObject(i);
                JSONObject urls = item.getJSONObject("urls");
                String imageUrl = urls.getStr("regular");

                // 获取图片宽高
                int width = item.getInt("width", 0);
                int height = item.getInt("height", 0);

                // 获取图片描述
                String description = item.getStr("alt_description");
                if (StrUtil.isBlank(description)) {
                    description = item.getStr("description");
                }
                if (StrUtil.isBlank(description)) {
                    description = originalSearchText + " 图片";
                }

                // 构建图片元数据
                Map<String, Object> metadata = buildPictureMetadata(
                        imageUrl,
                        width,
                        height,
                        "JPG",
                        description
                );

                // 添加用户信息
                if (item.containsKey("user")) {
                    JSONObject user = item.getJSONObject("user");
                    metadata.put("author", user.getStr("name", "Unsplash用户"));
                    metadata.put("source", "Unsplash");
                }

                result.add(metadata);
                log.debug("处理Unsplash图片: {}", imageUrl);
            }

            log.info("成功从Unsplash获取 {} 张图片", result.size());

            // 步骤4: 如果原始关键词和翻译关键词不同，且结果不足，尝试用原始关键词搜索
            if (result.size() < count && !originalSearchText.equals(searchText)) {
                log.info("翻译关键词搜索结果不足，尝试使用原始关键词 '{}'", originalSearchText);

                response = HttpRequest.get(API_URL)
                        .header("Authorization", "Client-ID " + apiKey)
                        .form("query", originalSearchText)
                        .form("per_page", count - result.size())
                        .execute();

                if (isValidResponse(response)) {
                    JSONObject jsonOriginal = JSONUtil.parseObj(response.body());
                    JSONArray resultsOriginal = jsonOriginal.getJSONArray("results");

                    for (int i = 0; i < resultsOriginal.size() && result.size() < count; i++) {
                        JSONObject item = resultsOriginal.getJSONObject(i);
                        JSONObject urls = item.getJSONObject("urls");
                        String imageUrl = urls.getStr("regular");

                        // 检查是否已经存在相同URL的图片
                        boolean isDuplicate = false;
                        for (Map<String, Object> existing : result) {
                            if (imageUrl.equals(existing.get("url"))) {
                                isDuplicate = true;
                                break;
                            }
                        }

                        if (isDuplicate) {
                            continue;
                        }

                        // 获取图片宽高
                        int width = item.getInt("width", 0);
                        int height = item.getInt("height", 0);

                        // 获取图片描述
                        String description = item.getStr("alt_description");
                        if (StrUtil.isBlank(description)) {
                            description = item.getStr("description");
                        }
                        if (StrUtil.isBlank(description)) {
                            description = originalSearchText + " 图片";
                        }

                        // 构建图片元数据
                        Map<String, Object> metadata = buildPictureMetadata(
                                imageUrl,
                                width,
                                height,
                                "JPG",
                                description
                        );

                        // 添加用户信息
                        if (item.containsKey("user")) {
                            JSONObject user = item.getJSONObject("user");
                            metadata.put("author", user.getStr("name", "Unsplash用户"));
                            metadata.put("source", "Unsplash");
                        }

                        result.add(metadata);
                    }
                }
            }

            // 步骤5: 确保返回确切数量的图片
            return ensureImageCount(result, originalSearchText, count);

        } catch (Exception e) {
            log.error("从Unsplash获取图片时发生错误", e);
            return ensureImageCount(result, originalSearchText, count);
        }
    }

    /**
     * 确保返回确切的请求图片数量
     */
    private List<Map<String, Object>> ensureImageCount(List<Map<String, Object>> currentResults, String searchText, int requiredCount) {
        // 如果已有足够的图片，截取所需数量
        if (currentResults.size() >= requiredCount) {
            return currentResults.subList(0, requiredCount);
        }

        // 否则，使用备用图片填充
        List<Map<String, Object>> finalResults = new ArrayList<>(currentResults);
        int remaining = requiredCount - currentResults.size();

        log.info("需要添加 {} 张备用图片来满足请求数量", remaining);

        for (int i = 0; i < remaining; i++) {
            // 选择合适的备用图片
            String fallbackUrl = chooseFallbackImage(searchText, i);

            // 构建图片元数据
            Map<String, Object> metadata = buildPictureMetadata(
                    fallbackUrl,
                    1080,  // Unsplash标准宽度
                    720,   // Unsplash标准高度
                    "JPG", // 标准格式
                    searchText + " 备用图片-" + (i + 1)
            );

            metadata.put("source", "Unsplash备用库");
            metadata.put("author", "Unsplash");
            metadata.put("isFallback", true);  // 标记为备用图片

            finalResults.add(metadata);
            log.debug("添加备用图片: {}", fallbackUrl);
        }

        log.info("最终返回 {} 张图片", finalResults.size());
        return finalResults;
    }

    /**
     * 选择合适的备用图片
     */
    private String chooseFallbackImage(String searchText, int index) {
        // 先尝试将搜索文本翻译成英文（如果是中文）
        String englishText = searchText;
        if (containsChinese(searchText)) {
            englishText = translateToEnglish(searchText);
        }

        // 常见类别映射
        Map<String, String> categoryMapping = new HashMap<>();
        categoryMapping.put("自然", "nature");
        categoryMapping.put("风景", "landscape");
        categoryMapping.put("人物", "people");
        categoryMapping.put("动物", "animal");
        categoryMapping.put("狗", "dog");
        categoryMapping.put("猫", "cat");
        categoryMapping.put("食物", "food");
        categoryMapping.put("建筑", "architecture");
        categoryMapping.put("科技", "technology");

        // 尝试根据中文关键词映射到英文类别
        for (Map.Entry<String, String> entry : categoryMapping.entrySet()) {
            if (searchText.contains(entry.getKey())) {
                return FALLBACK_IMAGES.getOrDefault(entry.getValue(), FALLBACK_IMAGES.get("default"));
            }
        }

        // 尝试根据英文关键词直接匹配
        for (String category : FALLBACK_IMAGES.keySet()) {
            if (englishText.toLowerCase().contains(category)) {
                return FALLBACK_IMAGES.get(category);
            }
        }

        // 如果没有匹配，使用默认图片
        return FALLBACK_IMAGES.get("default");
    }

    /**
     * 将中文搜索关键词翻译为英文
     */
    private String translateToEnglish(String chineseText) {
        // 如果不是中文，或者没有配置翻译API，直接返回原文
        if (!containsChinese(chineseText) || StrUtil.isBlank(translateAppId) || StrUtil.isBlank(translateSecret)) {
            return chineseText;
        }

        // 检查缓存
        if (TRANSLATION_CACHE.containsKey(chineseText)) {
            return TRANSLATION_CACHE.get(chineseText);
        }

        try {
            // 准备请求参数
            long salt = System.currentTimeMillis();
            String sign = generateTranslateSign(translateAppId, chineseText, salt, translateSecret);

            Map<String, Object> params = new HashMap<>();
            params.put("q", chineseText);
            params.put("from", "zh");
            params.put("to", "en");
            params.put("appid", translateAppId);
            params.put("salt", salt);
            params.put("sign", sign);

            // 发送翻译请求
            HttpResponse response = HttpRequest.post(BAIDU_TRANSLATE_API)
                    .form(params)
                    .timeout(5000)
                    .execute();

            if (!isValidResponse(response)) {
                log.warn("百度翻译API响应失败: HTTP {}", response.getStatus());
                return chineseText;
            }

            // 解析翻译结果
            JSONObject responseJson = JSONUtil.parseObj(response.body());

            if (!responseJson.containsKey("trans_result") || responseJson.getJSONArray("trans_result").isEmpty()) {
                log.warn("百度翻译API返回结果异常: {}", responseJson);
                return chineseText;
            }

            JSONArray transResults = responseJson.getJSONArray("trans_result");
            JSONObject firstResult = transResults.getJSONObject(0);
            String translatedText = firstResult.getStr("dst", chineseText);

            // 缓存翻译结果
            TRANSLATION_CACHE.put(chineseText, translatedText);

            return translatedText;

        } catch (Exception e) {
            log.error("调用百度翻译API出错", e);
            return chineseText;
        }
    }

    /**
     * 生成百度翻译API签名
     */
    private String generateTranslateSign(String appid, String query, long salt, String secKey) {
        String str = appid + query + salt + secKey;
        return md5(str);
    }

    /**
     * MD5加密
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            log.error("MD5加密出错", e);
            return "";
        }
    }

    /**
     * 检查文本是否包含中文字符
     */
    private boolean containsChinese(String text) {
        if (StrUtil.isBlank(text)) {
            return false;
        }

        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]");
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

    /**
     * 检查HTTP响应是否有效
     */
    public boolean isValidResponse(HttpResponse response) {
        return response != null && response.isOk() && StrUtil.isNotBlank(response.body());
    }

    /**
     * 处理爬取异常
     */
    public void handleCrawlException(Exception e, String source) {
        log.error("从{}获取图片时发生错误: {}", source, e.getMessage());
        if (e instanceof BusinessException) {
            throw (BusinessException) e;
        } else {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取图片时发生错误: " + e.getMessage());
        }
    }
}