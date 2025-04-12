package org.leocoder.picture.manager.crawler.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 16:55
 * @description : Wallhaven图片抓取实现（支持中文关键词翻译）
 */
@Slf4j
@Component
public class WallhavenPictureCrawler extends BasePictureCrawler {

    // Wallhaven搜索URL
    private static final String WALLHAVEN_SEARCH_URL = "https://wallhaven.cc/search";

    // 用户代理字符串
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36";

    // 百度翻译API
    private static final String BAIDU_TRANSLATE_API = "https://fanyi-api.baidu.com/api/trans/vip/translate";

    // 翻译结果缓存
    private static final Map<String, String> TRANSLATION_CACHE = new ConcurrentHashMap<>();

    // 备用图片URLs（按类别）
    private static final Map<String, String> FALLBACK_IMAGES = new HashMap<>();

    // 百度翻译API配置
    @Value("${picture.crawler.baidu.translate.appid:}")
    private String translateAppId;

    @Value("${picture.crawler.baidu.translate.secret:}")
    private String translateSecret;

    // Wallhaven API密钥（可选，用于访问NSFW内容）
    @Value("${picture.crawler.wallhaven.api-key:}")
    private String wallhavenApiKey;

    // 是否验证图片URL可访问性
    @Value("${picture.crawler.verify-image-url:false}")
    private boolean verifyImageUrl;

    // 备用图片获取超时时间(毫秒)
    @Value("${picture.crawler.image-timeout:5000}")
    private int imageTimeout;

    static {
        // 初始化备用图片集合 - 按类别分类的通用壁纸
        FALLBACK_IMAGES.put("自然", "https://w.wallhaven.cc/full/9m/wallhaven-9mjoy1.png");
        FALLBACK_IMAGES.put("风景", "https://w.wallhaven.cc/full/3z/wallhaven-3z9dv3.jpg");
        FALLBACK_IMAGES.put("动物", "https://w.wallhaven.cc/full/73/wallhaven-7356o3.jpg");
        FALLBACK_IMAGES.put("猫", "https://w.wallhaven.cc/full/28/wallhaven-28vg9g.jpg");
        FALLBACK_IMAGES.put("狗", "https://w.wallhaven.cc/full/72/wallhaven-72rd8e.jpg");
        FALLBACK_IMAGES.put("科技", "https://w.wallhaven.cc/full/l8/wallhaven-l83o92.jpg");
        FALLBACK_IMAGES.put("游戏", "https://w.wallhaven.cc/full/vq/wallhaven-vqz7qm.jpg");
        FALLBACK_IMAGES.put("艺术", "https://w.wallhaven.cc/full/z8/wallhaven-z8dg9y.jpg");
        FALLBACK_IMAGES.put("建筑", "https://w.wallhaven.cc/full/nz/wallhaven-nzxq1k.jpg");
        FALLBACK_IMAGES.put("default", "https://w.wallhaven.cc/full/we/wallhaven-weqmgx.jpg");
    }

    @Override
    public String getSourceName() {
        return "wallhaven";
    }

    @Override
    public List<Map<String, Object>> crawlPictures(String searchText, Integer count) {
        List<Map<String, Object>> result = new ArrayList<>();
        String originalSearchText = searchText;

        try {
            // 步骤1: 如果是中文关键词，翻译成英文
            String translatedSearchText = translateToEnglish(searchText);

            if (!searchText.equals(translatedSearchText)) {
                log.info("将搜索关键词从 '{}' 翻译为 '{}'", searchText, translatedSearchText);
                searchText = translatedSearchText;
            }

            log.info("开始从Wallhaven搜索 '{}' 相关图片，数量: {}", searchText, count);

            // 步骤2: 构建搜索URL
            String encodedQuery = URLEncoder.encode(searchText, StandardCharsets.UTF_8.name());
            String url = WALLHAVEN_SEARCH_URL + "?q=" + encodedQuery;

            // 添加排序和过滤参数
            url += "&sorting=relevance&order=desc";

            // 设置成人内容过滤（根据API密钥是否存在）
            String purity = StrUtil.isBlank(wallhavenApiKey) ? "100" : "110";  // 100=SFW, 110=SFW+Sketchy, 111=全部
            url += "&purity=" + purity;

            // 添加API密钥（如果存在）
            if (!StrUtil.isBlank(wallhavenApiKey)) {
                url += "&apikey=" + wallhavenApiKey;
            }

            log.debug("发送Wallhaven搜索请求: {}", url);

            // 步骤3: 发送HTTP请求并获取HTML
            HttpResponse response = HttpRequest.get(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Connection", "keep-alive")
                    .timeout(15000)
                    .execute();

            // 检查响应是否成功
            if (!isValidResponse(response)) {
                log.error("Wallhaven搜索页面请求失败: HTTP {}", response.getStatus());
                return ensureImageCount(result, originalSearchText, count);
            }

            String html = response.body();

            // 步骤4: 使用JSoup解析HTML
            Document doc = Jsoup.parse(html);
            Elements thumbnails = doc.select("figure.thumb");

            log.info("从Wallhaven搜索页面获取到 {} 个缩略图", thumbnails.size());

            // 处理每个缩略图 - 尝试获取2倍数量的图片，以便后续筛选
            for (int i = 0; i < thumbnails.size() && result.size() < count * 2; i++) {
                Element thumb = thumbnails.get(i);

                try {
                    // 获取壁纸ID和详情页URL
                    String wallpaperId = thumb.attr("data-wallpaper-id");
                    if (StrUtil.isBlank(wallpaperId)) {
                        continue;
                    }

                    // 获取预览图URL
                    Element preview = thumb.selectFirst("img.lazyload");
                    if (preview == null) {
                        continue;
                    }

                    String previewUrl = preview.attr("data-src");
                    if (StrUtil.isBlank(previewUrl)) {
                        continue;
                    }

                    // 构建完整图片URL
                    // Wallhaven的URL格式：https://w.wallhaven.cc/full/{前两位}/{id}.{扩展名}
                    String extension = getExtensionFromUrl(previewUrl);
                    String fullImageUrl = constructFullImageUrl(wallpaperId, extension);

                    // 验证图片URL是否可访问（如果配置了验证选项）
                    if (verifyImageUrl) {
                        boolean isImageAccessible = verifyImageUrl(fullImageUrl);
                        if (!isImageAccessible) {
                            log.warn("图片URL无法访问，跳过: {}", fullImageUrl);
                            continue;
                        }
                    }

                    // 获取图片尺寸
                    Element resolution = thumb.selectFirst("span.wall-res");
                    int width = 0;
                    int height = 0;

                    if (resolution != null) {
                        String resText = resolution.text();
                        String[] dimensions = resText.split(" x ");
                        if (dimensions.length == 2) {
                            try {
                                width = Integer.parseInt(dimensions[0]);
                                height = Integer.parseInt(dimensions[1]);
                            } catch (NumberFormatException e) {
                                log.warn("无法解析图片尺寸: {}", resText);
                            }
                        }
                    }

                    // 获取图片标签
                    StringBuilder tagsBuilder = new StringBuilder();
                    Element tagsElement = thumb.selectFirst("a.preview");
                    if (tagsElement != null) {
                        String dataTags = tagsElement.attr("data-tags");
                        if (!StrUtil.isBlank(dataTags)) {
                            tagsBuilder.append(dataTags);
                        }
                    }

                    String tags = tagsBuilder.toString();
                    String title = StrUtil.isBlank(tags) ? originalSearchText + " 壁纸" : tags;

                    // 构建图片元数据
                    Map<String, Object> metadata = buildPictureMetadata(
                            fullImageUrl,
                            width,
                            height,
                            extension,
                            title
                    );

                    // 添加源信息
                    metadata.put("source", "Wallhaven");
                    metadata.put("wallpaperId", wallpaperId);
                    if (!StrUtil.isBlank(tags)) {
                        metadata.put("tags", tags);
                    }

                    result.add(metadata);
                    log.debug("处理Wallhaven图片: {}", fullImageUrl);

                } catch (Exception e) {
                    log.warn("处理Wallhaven缩略图时出错: {}", e.getMessage());
                    // 继续处理下一个
                }
            }

            log.info("成功从Wallhaven获取 {} 张图片", result.size());

            // 步骤5: 如果原始关键词和翻译关键词不同，且结果不足，尝试用原始关键词搜索
            if (result.size() < count && !originalSearchText.equals(searchText)) {
                log.info("翻译关键词搜索结果不足，尝试使用原始关键词 '{}'", originalSearchText);

                // 构建使用原始关键词的URL
                encodedQuery = URLEncoder.encode(originalSearchText, StandardCharsets.UTF_8.name());
                String originalUrl = WALLHAVEN_SEARCH_URL + "?q=" + encodedQuery
                        + "&sorting=relevance&order=desc&purity=" + purity;

                if (!StrUtil.isBlank(wallhavenApiKey)) {
                    originalUrl += "&apikey=" + wallhavenApiKey;
                }

                response = HttpRequest.get(originalUrl)
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .timeout(15000)
                        .execute();

                if (isValidResponse(response)) {
                    Document originalDoc = Jsoup.parse(response.body());
                    Elements originalThumbnails = originalDoc.select("figure.thumb");

                    for (int i = 0; i < originalThumbnails.size() && result.size() < count * 2; i++) {
                        Element thumb = originalThumbnails.get(i);

                        try {
                            // 获取壁纸ID
                            String wallpaperId = thumb.attr("data-wallpaper-id");
                            if (StrUtil.isBlank(wallpaperId)) {
                                continue;
                            }

                            // 检查是否已经存在相同ID的图片
                            boolean isDuplicate = false;
                            for (Map<String, Object> item : result) {
                                if (wallpaperId.equals(item.get("wallpaperId"))) {
                                    isDuplicate = true;
                                    break;
                                }
                            }

                            if (isDuplicate) {
                                continue;
                            }

                            // 获取预览图URL
                            Element preview = thumb.selectFirst("img.lazyload");
                            if (preview == null) {
                                continue;
                            }

                            String previewUrl = preview.attr("data-src");
                            if (StrUtil.isBlank(previewUrl)) {
                                continue;
                            }

                            // 构建完整图片URL
                            String extension = getExtensionFromUrl(previewUrl);
                            String fullImageUrl = constructFullImageUrl(wallpaperId, extension);

                            // 验证图片URL是否可访问（如果配置了验证选项）
                            if (verifyImageUrl) {
                                boolean isImageAccessible = verifyImageUrl(fullImageUrl);
                                if (!isImageAccessible) {
                                    log.warn("图片URL无法访问，跳过: {}", fullImageUrl);
                                    continue;
                                }
                            }

                            // 获取图片尺寸
                            Element resolution = thumb.selectFirst("span.wall-res");
                            int width = 0;
                            int height = 0;

                            if (resolution != null) {
                                String resText = resolution.text();
                                String[] dimensions = resText.split(" x ");
                                if (dimensions.length == 2) {
                                    try {
                                        width = Integer.parseInt(dimensions[0]);
                                        height = Integer.parseInt(dimensions[1]);
                                    } catch (NumberFormatException e) {
                                        log.warn("无法解析图片尺寸: {}", resText);
                                    }
                                }
                            }

                            // 获取图片标签
                            StringBuilder tagsBuilder = new StringBuilder();
                            Element tagsElement = thumb.selectFirst("a.preview");
                            if (tagsElement != null) {
                                String dataTags = tagsElement.attr("data-tags");
                                if (!StrUtil.isBlank(dataTags)) {
                                    tagsBuilder.append(dataTags);
                                }
                            }

                            String tags = tagsBuilder.toString();
                            String title = StrUtil.isBlank(tags) ? originalSearchText + " 壁纸" : tags;

                            // 构建图片元数据
                            Map<String, Object> metadata = buildPictureMetadata(
                                    fullImageUrl,
                                    width,
                                    height,
                                    extension,
                                    title
                            );

                            // 添加源信息
                            metadata.put("source", "Wallhaven");
                            metadata.put("wallpaperId", wallpaperId);
                            if (!StrUtil.isBlank(tags)) {
                                metadata.put("tags", tags);
                            }

                            result.add(metadata);
                        } catch (Exception e) {
                            log.warn("处理Wallhaven缩略图(原始关键词)时出错: {}", e.getMessage());
                        }
                    }
                }
            }

            // 步骤6: 尝试从其他页面获取更多结果（如果仍然不足）
            if (result.size() < count) {
                log.info("从首页获取的图片数量不足，尝试加载更多页面");

                // 尝试最多加载3个额外页面
                for (int page = 2; page <= 4 && result.size() < count * 1.5; page++) {
                    String pageUrl = url + "&page=" + page;

                    try {
                        HttpResponse pageResponse = HttpRequest.get(pageUrl)
                                .header("User-Agent", USER_AGENT)
                                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                                .timeout(15000)
                                .execute();

                        if (isValidResponse(pageResponse)) {
                            Document pageDoc = Jsoup.parse(pageResponse.body());
                            Elements pageThumbnails = pageDoc.select("figure.thumb");

                            // 处理额外页面的缩略图
                            for (int i = 0; i < pageThumbnails.size() && result.size() < count * 1.5; i++) {
                                Element thumb = pageThumbnails.get(i);

                                try {
                                    // 和上面的代码类似，提取图片信息
                                    String wallpaperId = thumb.attr("data-wallpaper-id");
                                    if (StrUtil.isBlank(wallpaperId)) {
                                        continue;
                                    }

                                    // 检查是否已经存在相同ID的图片
                                    boolean isDuplicate = false;
                                    for (Map<String, Object> item : result) {
                                        if (wallpaperId.equals(item.get("wallpaperId"))) {
                                            isDuplicate = true;
                                            break;
                                        }
                                    }

                                    if (isDuplicate) {
                                        continue;
                                    }

                                    Element preview = thumb.selectFirst("img.lazyload");
                                    if (preview == null) {
                                        continue;
                                    }

                                    String previewUrl = preview.attr("data-src");
                                    if (StrUtil.isBlank(previewUrl)) {
                                        continue;
                                    }

                                    String extension = getExtensionFromUrl(previewUrl);
                                    String fullImageUrl = constructFullImageUrl(wallpaperId, extension);

                                    // 获取图片尺寸
                                    Element resolution = thumb.selectFirst("span.wall-res");
                                    int width = 0;
                                    int height = 0;

                                    if (resolution != null) {
                                        String resText = resolution.text();
                                        String[] dimensions = resText.split(" x ");
                                        if (dimensions.length == 2) {
                                            try {
                                                width = Integer.parseInt(dimensions[0]);
                                                height = Integer.parseInt(dimensions[1]);
                                            } catch (NumberFormatException e) {
                                                log.warn("无法解析图片尺寸: {}", resText);
                                            }
                                        }
                                    }

                                    StringBuilder tagsBuilder = new StringBuilder();
                                    Element tagsElement = thumb.selectFirst("a.preview");
                                    if (tagsElement != null) {
                                        String dataTags = tagsElement.attr("data-tags");
                                        if (!StrUtil.isBlank(dataTags)) {
                                            tagsBuilder.append(dataTags);
                                        }
                                    }

                                    String tags = tagsBuilder.toString();
                                    String title = StrUtil.isBlank(tags) ? originalSearchText + " 壁纸" : tags;

                                    Map<String, Object> metadata = buildPictureMetadata(
                                            fullImageUrl,
                                            width,
                                            height,
                                            extension,
                                            title
                                    );

                                    metadata.put("source", "Wallhaven");
                                    metadata.put("wallpaperId", wallpaperId);
                                    if (!StrUtil.isBlank(tags)) {
                                        metadata.put("tags", tags);
                                    }

                                    result.add(metadata);
                                } catch (Exception e) {
                                    log.warn("处理额外页面的缩略图时出错: {}", e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("加载额外页面 {} 时出错: {}", page, e.getMessage());
                    }
                }
            }

            // 确保返回确切的请求数量
            return ensureImageCount(result, originalSearchText, count);

        } catch (Exception e) {
            log.error("从Wallhaven获取图片时发生错误", e);
            return ensureImageCount(result, originalSearchText, count);
        }
    }

    /**
     * 从URL中获取图片扩展名
     */
    private String getExtensionFromUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return "jpg";  // 默认扩展名
        }

        Pattern pattern = Pattern.compile("\\.([a-zA-Z0-9]+)($|\\?|&)");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }

        return "jpg";  // 默认返回jpg
    }

    /**
     * 根据壁纸ID和扩展名构建完整图片URL
     */
    private String constructFullImageUrl(String wallpaperId, String extension) {
        // Wallhaven完整图片URL格式: https://w.wallhaven.cc/full/{前两位}/{id}.{扩展名}
        if (wallpaperId.length() < 2) {
            return null;
        }

        String prefix = wallpaperId.substring(0, 2);
        return "https://w.wallhaven.cc/full/" + prefix + "/wallhaven-" + wallpaperId + "." + extension;
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

        // 创建一个备用图片列表，用于随机选择，避免重复使用同一张备用图片
        List<String> availableFallbacks = new ArrayList<>(FALLBACK_IMAGES.values());
        if (availableFallbacks.isEmpty()) {
            // 如果没有预设的备用图片，添加一个默认图片
            availableFallbacks.add("https://w.wallhaven.cc/full/we/wallhaven-weqmgx.jpg");
        }

        for (int i = 0; i < remaining; i++) {
            try {
                // 选择合适的备用图片
                String fallbackUrl = chooseFallbackImage(searchText, i, availableFallbacks);

                // 构建图片元数据
                Map<String, Object> metadata = buildPictureMetadata(
                        fallbackUrl,
                        1920,  // 标准宽度
                        1080,  // 标准高度
                        getExtensionFromUrl(fallbackUrl),
                        searchText + " 备用壁纸-" + (i + 1)
                );

                metadata.put("source", "Wallhaven备用库");
                metadata.put("isFallback", true);  // 标记为备用图片
                metadata.put("wallpaperId", "fallback-" + System.currentTimeMillis() + "-" + i);  // 生成唯一ID

                finalResults.add(metadata);
                log.debug("添加备用图片: {}", fallbackUrl);

                // 随机延迟，避免过快添加备用图片
                if (i < remaining - 1) {
                    TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(50, 150));
                }
            } catch (Exception e) {
                log.warn("添加备用图片时出错: {}", e.getMessage());
                // 继续添加下一张备用图片
            }
        }

        log.info("最终返回 {} 张图片", finalResults.size());

        // 最后进行一次数量检查，确保绝对等于要求的数量
        if (finalResults.size() != requiredCount) {
            log.warn("图片数量不匹配，要求: {}, 实际: {}, 进行强制调整", requiredCount, finalResults.size());
            if (finalResults.size() > requiredCount) {
                return finalResults.subList(0, requiredCount);
            } else {
                // 这种情况理论上不应该发生，但为了严谨，添加处理
                int shortfall = requiredCount - finalResults.size();
                for (int i = 0; i < shortfall; i++) {
                    try {
                        Map<String, Object> metadata = buildPictureMetadata(
                                FALLBACK_IMAGES.getOrDefault("default", "https://w.wallhaven.cc/full/we/wallhaven-weqmgx.jpg"),
                                1920,
                                1080,
                                "jpg",
                                searchText + " 紧急备用壁纸-" + i
                        );
                        metadata.put("source", "Wallhaven紧急备用");
                        metadata.put("isFallback", true);
                        metadata.put("isEmergencyFallback", true);
                        finalResults.add(metadata);
                    } catch (Exception e) {
                        log.error("添加紧急备用图片时出错: {}", e.getMessage());
                        // 创建一个绝对不会失败的最后手段备用图片
                        Map<String, Object> lastResort = new HashMap<>();
                        lastResort.put("url", "https://w.wallhaven.cc/full/we/wallhaven-weqmgx.jpg");
                        lastResort.put("width", 1920);
                        lastResort.put("height", 1080);
                        lastResort.put("format", "jpg");
                        lastResort.put("title", searchText + " 最终备用图片");
                        lastResort.put("source", "Wallhaven最终备用");
                        lastResort.put("isFallback", true);
                        lastResort.put("isLastResort", true);
                        finalResults.add(lastResort);
                    }
                }
            }
        }

        // 无论如何，确保返回确切的请求数量
        while (finalResults.size() > requiredCount) {
            finalResults.remove(finalResults.size() - 1);
        }

        return finalResults;
    }

    /**
     * 选择合适的备用图片
     * @param searchText 搜索文本
     * @param index 当前索引
     * @param availableFallbacks 可用的备用图片列表
     * @return 备用图片URL
     */
    private String chooseFallbackImage(String searchText, int index, List<String> availableFallbacks) {
        // 首先尝试按类别匹配
        for (Map.Entry<String, String> entry : FALLBACK_IMAGES.entrySet()) {
            if (!entry.getKey().equals("default") && searchText.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 如果没有匹配到类别，则从可用备用图片中随机选择
        if (!availableFallbacks.isEmpty()) {
            int randomIndex = ThreadLocalRandom.current().nextInt(availableFallbacks.size());
            return availableFallbacks.get(randomIndex);
        }

        // 最后兜底，使用默认图片
        return FALLBACK_IMAGES.getOrDefault("default", "https://w.wallhaven.cc/full/we/wallhaven-weqmgx.jpg");
    }

    /**
     * 验证图片URL是否可访问
     * @param imageUrl 图片URL
     * @return 是否可访问
     */
    private boolean verifyImageUrl(String imageUrl) {
        if (StrUtil.isBlank(imageUrl)) {
            return false;
        }

        try {
            // 发送HEAD请求验证URL是否有效，减少带宽消耗
            HttpResponse response = HttpRequest.head(imageUrl)
                    .header("User-Agent", USER_AGENT)
                    .timeout(imageTimeout)  // 使用配置的超时时间
                    .execute();

            return response != null && response.isOk();
        } catch (Exception e) {
            log.debug("验证图片URL时出错: {}, {}", imageUrl, e.getMessage());
            return false;
        }
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
}