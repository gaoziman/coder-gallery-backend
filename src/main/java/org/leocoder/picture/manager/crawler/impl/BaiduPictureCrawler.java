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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 16:55
 * @description : 百度图片抓取实现 (完全模拟浏览器 + 中英翻译)
 */
@Slf4j
@Component
public class BaiduPictureCrawler extends BasePictureCrawler {

    // 百度图片搜索首页
    private static final String BAIDU_IMAGE_INDEX = "https://image.baidu.com";

    // 百度图片搜索结果页
    private static final String BAIDU_IMAGE_SEARCH = "https://image.baidu.com/search/index";

    // 百度图片搜索JSON API
    private static final String BAIDU_IMAGE_SEARCH_API = "https://image.baidu.com/search/acjson";

    // 百度翻译API
    private static final String BAIDU_TRANSLATE_API = "https://fanyi-api.baidu.com/api/trans/vip/translate";

    // 用户代理字符串 - 使用最新的Chrome浏览器UA
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36";

    // 备用图片URL - 当无法获取足够图片时使用
    private static final Map<String, String> FALLBACK_IMAGES = new HashMap<>();

    // 翻译结果缓存
    private static final Map<String, String> TRANSLATION_CACHE = new ConcurrentHashMap<>();

    // 百度翻译API配置
    @Value("${picture.crawler.baidu.translate.appid:}")
    private String translateAppId;

    @Value("${picture.crawler.baidu.translate.secret:}")
    private String translateSecret;

    static {
        // 初始化备用图片集合 - 按类别分类的通用图片
        FALLBACK_IMAGES.put("动物", "https://img1.baidu.com/it/u=2745651171,3785699900&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500");
        FALLBACK_IMAGES.put("狗", "https://img0.baidu.com/it/u=3304799215,209213683&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500");
        FALLBACK_IMAGES.put("猫", "https://img2.baidu.com/it/u=3342004910,1501944046&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500");
        FALLBACK_IMAGES.put("人物", "https://img1.baidu.com/it/u=2658823123,1186193495&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500");
        FALLBACK_IMAGES.put("风景", "https://img0.baidu.com/it/u=1695309667,2317920401&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500");
        FALLBACK_IMAGES.put("食物", "https://img1.baidu.com/it/u=2687337657,3882460042&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500");
        FALLBACK_IMAGES.put("建筑", "https://img2.baidu.com/it/u=911206241,3840047041&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500");
        FALLBACK_IMAGES.put("科技", "https://img2.baidu.com/it/u=1582433641,3557641584&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500");
        FALLBACK_IMAGES.put("default", "https://img0.baidu.com/it/u=3245196761,3178194566&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500");
    }

    @Override
    public String getSourceName() {
        return "baidu";
    }

    @Override
    public List<Map<String, Object>> crawlPictures(String searchText, Integer count) {
        List<Map<String, Object>> result = new ArrayList<>();

        String originalSearchText = null;
        try {
            log.info("开始从百度搜索 '{}' 相关图片，数量: {}", searchText, count);

            // 步骤0: 翻译关键词 (中文 -> 英文)
            originalSearchText = searchText;
            String translatedSearchText = translateToEnglish(searchText);

            if (!searchText.equals(translatedSearchText)) {
                log.info("将搜索关键词从 '{}' 翻译为 '{}'", searchText, translatedSearchText);
                searchText = translatedSearchText;
            }

            // 步骤1: 获取必要的Cookie和初始页面
            Map<String, String> cookies = getInitialCookies();
            if (cookies.isEmpty()) {
                log.error("无法获取百度初始Cookie，可能被反爬虫机制拦截");
                return ensureImageCount(result, originalSearchText, count);
            }

            // 步骤2: 访问搜索页面获取更多Cookie
            String encodedQuery = URLEncoder.encode(searchText, StandardCharsets.UTF_8.name());
            cookies = getSearchPageCookies(encodedQuery, cookies);

            // 步骤3: 构建JSON API请求
            Map<String, Object> params = buildSearchParams(encodedQuery, Math.max(count * 2, 30)); // 请求更多图片以确保有足够的有效结果
            String queryString = buildQueryString(params);
            String url = BAIDU_IMAGE_SEARCH_API + "?" + queryString;

            log.debug("发送百度图像搜索请求: {}", url);
            log.debug("使用Cookie: {}", cookies);

            // 步骤4: 执行搜索请求
            HttpResponse response = HttpRequest.get(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/json,application/xhtml+xml")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Referer", BAIDU_IMAGE_SEARCH + "?tn=baiduimage&word=" + encodedQuery)
                    .header("sec-ch-ua", "\"Chromium\";v=\"110\", \"Not A(Brand\";v=\"24\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"Windows\"")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Cookie", formatCookies(cookies))
                    .timeout(15000)
                    .execute();

            // 步骤5: 检查和解析响应
            if (!isValidResponse(response)) {
                log.error("百度图像搜索响应失败: HTTP {}", response.getStatus());
                // 尝试直接从搜索页面解析结果作为备用方案
                result = extractImagesFromSearchPage(searchText, count * 2, cookies);
                return ensureImageCount(result, originalSearchText, count);
            }

            // 解析JSON响应
            String responseBody = response.body();

            // 调试输出响应前100个字符，帮助诊断
            log.debug("响应内容前100个字符: {}", StrUtil.sub(responseBody, 0, 100));

            if (!responseBody.startsWith("{")) {
                log.error("返回的不是有效的JSON格式，尝试备用方案");
                result = extractImagesFromSearchPage(searchText, count * 2, cookies);
                return ensureImageCount(result, originalSearchText, count);
            }

            JSONObject responseJson = JSONUtil.parseObj(responseBody);

            // 检查响应数据
            if (!responseJson.containsKey("data") || responseJson.getJSONArray("data").isEmpty()) {
                log.warn("百度API返回的data数组为空，尝试备用方法");
                result = extractImagesFromSearchPage(searchText, count * 2, cookies);
                return ensureImageCount(result, originalSearchText, count);
            }

            // 解析搜索结果
            JSONArray dataArray = responseJson.getJSONArray("data");
            int validResults = 0;
            for (int i = 0; i < dataArray.size(); i++) {
                if (dataArray.get(i) instanceof JSONObject && !((JSONObject) dataArray.get(i)).isEmpty()) {
                    validResults++;
                }
            }
            log.info("从百度获取到 {} 个有效图片搜索结果", validResults);

            // 处理每个结果项
            for (int i = 0; i < dataArray.size() && result.size() < count * 2; i++) {
                if (!(dataArray.get(i) instanceof JSONObject)) {
                    continue;
                }

                JSONObject item = dataArray.getJSONObject(i);
                if (item.isEmpty()) {
                    continue;
                }

                try {
                    // 尝试提取图片URL，按优先级顺序
                    String imageUrl = extractImageUrl(item);

                    if (StrUtil.isBlank(imageUrl)) {
                        continue;
                    }

                    // 获取图片元数据
                    int width = item.getInt("width", 0);
                    int height = item.getInt("height", 0);
                    String format = item.getStr("type", extractFormatFromUrl(imageUrl));

                    // 获取标题
                    String title = item.getStr("fromPageTitle", "");
                    if (StrUtil.isBlank(title)) {
                        title = originalSearchText + "图片";
                    }

                    // 构建元数据对象
                    Map<String, Object> metadata = buildPictureMetadata(
                            imageUrl,
                            width,
                            height,
                            format,
                            title
                    );

                    // 添加源站点信息
                    if (item.containsKey("fromURLHost")) {
                        metadata.put("source", item.getStr("fromURLHost"));
                    }

                    result.add(metadata);
                    log.debug("成功提取百度图片: {}", imageUrl);

                } catch (Exception e) {
                    log.warn("处理搜索结果项时出错: {}", e.getMessage());
                }
            }

            log.info("成功从百度获取 {} 张图片", result.size());

            // 如果没有获取到足够图片，尝试备用方法
            if (result.size() < count) {
                log.info("标准方法未获取到足够图片，尝试备用方法");
                List<Map<String, Object>> backupResults = extractImagesFromSearchPage(searchText, count * 2, cookies);

                // 添加备用结果，避免重复
                for (Map<String, Object> item : backupResults) {
                    if (result.size() >= count * 2) {
                        break;
                    }

                    String itemUrl = (String) item.get("url");
                    boolean isDuplicate = false;

                    for (Map<String, Object> existingItem : result) {
                        if (itemUrl.equals(existingItem.get("url"))) {
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (!isDuplicate) {
                        result.add(item);
                    }
                }
            }

            // 如果仍未获取到足够图片，尝试用原始中文关键词再搜索一次
            if (result.size() < count && !originalSearchText.equals(searchText)) {
                log.info("使用原始中文关键词 '{}' 再次搜索", originalSearchText);
                List<Map<String, Object>> chineseKeywordResults = crawlPicturesWithoutTranslation(originalSearchText, count * 2);

                // 添加中文关键词搜索结果，避免重复
                for (Map<String, Object> item : chineseKeywordResults) {
                    if (result.size() >= count * 2) {
                        break;
                    }

                    String itemUrl = (String) item.get("url");
                    boolean isDuplicate = false;

                    for (Map<String, Object> existingItem : result) {
                        if (itemUrl.equals(existingItem.get("url"))) {
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (!isDuplicate) {
                        result.add(item);
                    }
                }
            }

            // 确保返回确切的请求数量
            return ensureImageCount(result, originalSearchText, count);

        } catch (Exception e) {
            log.error("从百度获取图片时发生错误", e);
            return ensureImageCount(result, originalSearchText, count);
        }
    }

    /**
     * 不使用翻译的图片抓取方法（用于备用）
     */
    private List<Map<String, Object>> crawlPicturesWithoutTranslation(String searchText, Integer count) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            log.info("直接使用关键词 '{}' 搜索图片", searchText);

            // 获取Cookie
            Map<String, String> cookies = getInitialCookies();
            if (cookies.isEmpty()) {
                return result;
            }

            // 访问搜索页面获取更多Cookie
            String encodedQuery = URLEncoder.encode(searchText, StandardCharsets.UTF_8.name());
            cookies = getSearchPageCookies(encodedQuery, cookies);

            // 尝试直接从搜索页面提取图片
            result = extractImagesFromSearchPage(searchText, count, cookies);

        } catch (Exception e) {
            log.error("使用原始关键词搜索图片时出错", e);
        }

        return result;
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
                    500,  // 标准宽度
                    500,  // 标准高度
                    "jpg", // 标准格式
                    searchText + "备用图片-" + (i + 1)
            );

            metadata.put("source", "备用图库");
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
        // 尝试根据关键词匹配备用图片类别
        for (Map.Entry<String, String> entry : FALLBACK_IMAGES.entrySet()) {
            if (searchText.contains(entry.getKey())) {
                return entry.getValue();
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

            if (!response.isOk()) {
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
     * 获取初始Cookie
     */
    private Map<String, String> getInitialCookies() {
        Map<String, String> cookies = new HashMap<>();
        try {
            HttpResponse response = HttpRequest.get(BAIDU_IMAGE_INDEX)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .timeout(10000)
                    .execute();

            if (response.isOk()) {
                List<String> setCookies = response.headerList("Set-Cookie");
                for (String cookie : setCookies) {
                    String[] parts = cookie.split(";\\s*")[0].split("=", 2);
                    if (parts.length == 2) {
                        cookies.put(parts[0], parts[1]);
                    }
                }
                log.debug("成功获取初始Cookie: {}", cookies);
            } else {
                log.error("获取初始Cookie失败: HTTP {}", response.getStatus());
            }
        } catch (Exception e) {
            log.error("获取初始Cookie时出错", e);
        }
        return cookies;
    }

    /**
     * 获取搜索页面Cookie
     */
    private Map<String, String> getSearchPageCookies(String query, Map<String, String> existingCookies) {
        try {
            String url = BAIDU_IMAGE_SEARCH + "?tn=baiduimage&word=" + query;

            HttpResponse response = HttpRequest.get(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Connection", "keep-alive")
                    .header("Cookie", formatCookies(existingCookies))
                    .header("Upgrade-Insecure-Requests", "1")
                    .timeout(10000)
                    .execute();

            if (response.isOk()) {
                List<String> setCookies = response.headerList("Set-Cookie");
                for (String cookie : setCookies) {
                    String[] parts = cookie.split(";\\s*")[0].split("=", 2);
                    if (parts.length == 2) {
                        existingCookies.put(parts[0], parts[1]);
                    }
                }
                log.debug("成功获取搜索页面Cookie: {}", existingCookies);
            }
        } catch (Exception e) {
            log.error("获取搜索页面Cookie时出错", e);
        }
        return existingCookies;
    }

    /**
     * 构建搜索参数
     */
    private Map<String, Object> buildSearchParams(String query, int count) {
        Map<String, Object> params = new HashMap<>();
        params.put("tn", "resultjson_com");
        params.put("logid", String.valueOf(System.currentTimeMillis()));
        params.put("ipn", "rj");
        params.put("ct", "201326592");
        params.put("is", "");
        params.put("fp", "result");
        params.put("fr", "");
        params.put("word", query);
        params.put("queryWord", query);
        params.put("cl", "2");
        params.put("lm", "-1");
        params.put("ie", "utf-8");
        params.put("oe", "utf-8");
        params.put("adpicid", "");
        params.put("st", "-1");
        params.put("z", "");
        params.put("ic", "0");
        params.put("hd", "");
        params.put("latest", "");
        params.put("copyright", "");
        params.put("s", "");
        params.put("se", "");
        params.put("tab", "");
        params.put("width", "");
        params.put("height", "");
        params.put("face", "0");
        params.put("istype", "2");
        params.put("qc", "");
        params.put("nc", "1");
        params.put("expermode", "");
        params.put("nojc", "");
        params.put("pn", "0");  // 页码，从0开始
        params.put("rn", count);  // 每页结果数
        params.put("gsm", "1e");
        params.put("1644" + System.currentTimeMillis() % 100000, "");  // 时间戳参数

        return params;
    }

    /**
     * 将参数Map转换为查询字符串
     */
    private String buildQueryString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            try {
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
            } catch (Exception e) {
                // 忽略编码错误，直接添加
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        return sb.toString();
    }

    /**
     * 格式化Cookie为HTTP头格式
     */
    private String formatCookies(Map<String, String> cookies) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * 从搜索结果项中提取图片URL
     */
    private String extractImageUrl(JSONObject item) {
        // 1. 尝试从replaceUrl获取最高质量URL
        if (item.containsKey("replaceUrl") && item.getJSONArray("replaceUrl").size() > 0) {
            JSONObject replaceUrl = item.getJSONArray("replaceUrl").getJSONObject(0);
            if (replaceUrl.containsKey("ObjURL") && !StrUtil.isBlank(replaceUrl.getStr("ObjURL"))) {
                return replaceUrl.getStr("ObjURL");
            }
        }

        // 2. 尝试获取中等质量URL
        if (item.containsKey("middleURL") && !StrUtil.isBlank(item.getStr("middleURL"))) {
            return item.getStr("middleURL");
        }

        // 3. 尝试获取缩略图URL
        if (item.containsKey("thumbURL") && !StrUtil.isBlank(item.getStr("thumbURL"))) {
            return item.getStr("thumbURL");
        }

        // 4. 尝试获取悬停图URL
        if (item.containsKey("hoverURL") && !StrUtil.isBlank(item.getStr("hoverURL"))) {
            return item.getStr("hoverURL");
        }

        // 5. 尝试解密并获取objURL
        if (item.containsKey("objURL") && !StrUtil.isBlank(item.getStr("objURL"))) {
            String objURL = item.getStr("objURL");
            if (objURL.startsWith("ippr") || objURL.startsWith("ipprf")) {
                return decodeObjURL(objURL);
            } else {
                return objURL;
            }
        }

        // 没有找到有效URL
        return null;
    }

    /**
     * 备用方法：直接从搜索页面HTML中提取图片
     */
    private List<Map<String, Object>> extractImagesFromSearchPage(String searchText, int count, Map<String, String> cookies) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            log.info("使用备用方法：从搜索页面直接提取图片");

            String encodedQuery = URLEncoder.encode(searchText, StandardCharsets.UTF_8.name());
            String url = BAIDU_IMAGE_SEARCH + "?tn=baiduimage&word=" + encodedQuery;

            HttpResponse response = HttpRequest.get(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Cookie", formatCookies(cookies))
                    .timeout(15000)
                    .execute();

            if (!response.isOk()) {
                log.error("搜索页面请求失败: HTTP {}", response.getStatus());
                return result;
            }

            String html = response.body();

            // 提取页面中的数据
            Pattern dataPattern = Pattern.compile("\"data\"\\s*:\\s*(\\[.+?\\])[,\\}]", Pattern.DOTALL);
            Matcher dataMatcher = dataPattern.matcher(html);

            if (dataMatcher.find()) {
                String dataJson = dataMatcher.group(1);
                try {
                    JSONArray dataArray = JSONUtil.parseArray(dataJson);
                    log.info("从HTML中提取到 {} 个结果", dataArray.size());

                    for (int i = 0; i < dataArray.size() && result.size() < count; i++) {
                        if (!(dataArray.get(i) instanceof JSONObject)) {
                            continue;
                        }

                        JSONObject item = dataArray.getJSONObject(i);
                        if (item.isEmpty()) {
                            continue;
                        }

                        try {
                            String imageUrl = extractImageUrl(item);
                            if (StrUtil.isBlank(imageUrl)) {
                                continue;
                            }

                            // 获取图片尺寸和格式
                            int width = item.getInt("width", 0);
                            int height = item.getInt("height", 0);
                            String format = item.getStr("type", extractFormatFromUrl(imageUrl));

                            // 获取标题
                            String title = item.getStr("fromPageTitle", searchText + "图片");

                            // 构建元数据
                            Map<String, Object> metadata = buildPictureMetadata(
                                    imageUrl,
                                    width,
                                    height,
                                    format,
                                    title
                            );

                            result.add(metadata);
                            log.debug("从HTML成功提取图片: {}", imageUrl);

                        } catch (Exception e) {
                            log.warn("处理HTML搜索结果项时出错: {}", e.getMessage());
                        }
                    }

                } catch (Exception e) {
                    log.error("解析HTML中的JSON数据时出错", e);
                }
            } else {
                log.warn("在HTML中未找到图片数据，尝试使用正则表达式提取");

                // 尝试直接提取图片URL
                Pattern imgPattern = Pattern.compile("\"thumbURL\"\\s*:\\s*\"([^\"]+)\"");
                Matcher imgMatcher = imgPattern.matcher(html);

                int count2 = 0;
                while (imgMatcher.find() && count2 < count) {
                    String imgUrl = imgMatcher.group(1);
                    if (!StrUtil.isBlank(imgUrl)) {
                        Map<String, Object> metadata = buildPictureMetadata(
                                imgUrl,
                                0,
                                0,
                                extractFormatFromUrl(imgUrl),
                                searchText + "图片"
                        );
                        result.add(metadata);
                        count2++;
                    }
                }

                log.info("通过正则表达式提取到 {} 张图片", count2);
            }

        } catch (Exception e) {
            log.error("使用备用方法提取图片时出错", e);
        }

        return result;
    }

    /**
     * 解密百度objURL
     */
    private String decodeObjURL(String objURL) {
        try {
            if (StrUtil.isBlank(objURL)) {
                return "";
            }

            // 百度URL解密映射表
            Map<Character, Character> decodeMap = new HashMap<>();
            String map1 = "wkv1jia0tbzr9gqx2ps8d7e3yfnh4umo5lc6";
            String map2 = "abcdefghijklmnopqrstuvw1234567890";

            for (int i = 0; i < map1.length() && i < map2.length(); i++) {
                decodeMap.put(map1.charAt(i), map2.charAt(i));
            }

            // 第一步：替换特定字符
            String decoded = objURL.replace("_z2C$q", ":")
                    .replace("_z&e3B", ".")
                    .replace("AzdH3F", "/");

            // 第二步：应用字符映射
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < decoded.length(); i++) {
                char c = decoded.charAt(i);
                if (decodeMap.containsKey(c)) {
                    sb.append(decodeMap.get(c));
                } else {
                    sb.append(c);
                }
            }

            return sb.toString();

        } catch (Exception e) {
            log.warn("解密百度objURL失败: {}", e.getMessage());
            return objURL;
        }
    }

    /**
     * 从URL提取图片格式
     */
    public String extractFormatFromUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return "jpg";
        }

        Pattern pattern = Pattern.compile("\\.(jpg|jpeg|png|gif|webp|bmp)($|\\?|&)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }

        return "jpg";
    }

    /**
     * 检查响应是否有效
     */
    public boolean isValidResponse(HttpResponse response) {
        if (response == null || !response.isOk()) {
            return false;
        }

        String body = response.body();
        return !StrUtil.isBlank(body) && body.length() > 10;
    }
}