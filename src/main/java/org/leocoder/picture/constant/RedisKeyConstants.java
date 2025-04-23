package org.leocoder.picture.constant;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-23 16:32
 * @description : Redis键常量管理类
 */
public class RedisKeyConstants {
    // 图片模块前缀
    public static final String PICTURE_PREFIX = "picture:";
    
    // 瀑布流相关键前缀
    public static final String WATERFALL_PREFIX = PICTURE_PREFIX + "waterfall:";
    public static final String COUNT_PREFIX = PICTURE_PREFIX + "count:";
    
    // 缓存过期时间（分钟）
    public static final int WATERFALL_CACHE_EXPIRE_MINUTES = 30;
    public static final int WATERFALL_MORE_CACHE_EXPIRE_MINUTES = 5;
    public static final int COUNT_CACHE_EXPIRE_MINUTES = 5;
    
    /**
     * 构建瀑布流缓存键
     */
    public static String buildWaterfallKey(String prefix, String sortBy, Integer pageSize,
                                         Long categoryId, List<Long> tagIds,
                                         String format, Integer minWidth, Integer minHeight,
                                         Long userId, String keyword) {
        StringBuilder keyBuilder = new StringBuilder(WATERFALL_PREFIX);
        keyBuilder.append(prefix).append(":");
        keyBuilder.append(sortBy).append(":");
        keyBuilder.append(pageSize).append(":");
        
        // 添加筛选条件
        if (categoryId != null) {
            keyBuilder.append("c").append(categoryId).append(":");
        }
        
        if (CollUtil.isNotEmpty(tagIds)) {
            keyBuilder.append("t");
            for (Long tagId : tagIds) {
                keyBuilder.append(tagId).append(",");
            }
            keyBuilder.append(":");
        }
        
        if (StrUtil.isNotBlank(format)) {
            keyBuilder.append("f").append(format).append(":");
        }
        
        if (minWidth != null) {
            keyBuilder.append("w").append(minWidth).append(":");
        }
        
        if (minHeight != null) {
            keyBuilder.append("h").append(minHeight).append(":");
        }
        
        if (userId != null) {
            keyBuilder.append("u").append(userId).append(":");
        }
        
        if (StrUtil.isNotBlank(keyword)) {
            keyBuilder.append("k").append(keyword);
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 构建图片计数缓存键
     */
    public static String buildCountKey(String format, Integer minWidth, Integer minHeight,
                                     Long userId, Long categoryId, List<Long> tagIds, String keyword) {
        StringBuilder countKey = new StringBuilder(COUNT_PREFIX);
        
        if (StrUtil.isNotBlank(format)) countKey.append("f").append(format).append(":");
        if (minWidth != null) countKey.append("w").append(minWidth).append(":");
        if (minHeight != null) countKey.append("h").append(minHeight).append(":");
        if (userId != null) countKey.append("u").append(userId).append(":");
        if (categoryId != null) countKey.append("c").append(categoryId).append(":");
        
        if (CollUtil.isNotEmpty(tagIds)) {
            countKey.append("t");
            for (Long tagId : tagIds) countKey.append(tagId).append(",");
            countKey.append(":");
        }
        
        if (StrUtil.isNotBlank(keyword)) countKey.append("k").append(keyword);
        
        return countKey.toString();
    }
}