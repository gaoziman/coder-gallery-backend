package org.leocoder.picture.cache;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.constant.RedisKeyConstants;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-24 10:00
 * @description : 图片缓存管理器 - 处理缓存一致性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PictureCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // 阈值管理 - 避免频繁清除缓存
    private final ConcurrentHashMap<Long, AtomicInteger> viewCountThresholds = new ConcurrentHashMap<>();
    // 每5次浏览增加才清除缓存
    private static final int VIEW_COUNT_THRESHOLD = 5;
    

    /**
     * 图片上传后清除相关缓存
     * @param userId 用户ID
     */
    public void invalidateAfterPictureUpload(Long userId) {
        // 清除所有初始瀑布流页面的缓存，包括各种排序方式
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "initial:*");

        // 仍然特别清除"最新"排序的缓存，确保肯定被清除
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*newest*");

        // 清除首页可能使用的其他排序方式
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*popular*");
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*mostViewed*");
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*mostLiked*");
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*mostCollected*");

        // 如果按用户筛选，清除该用户的缓存（保持原有逻辑）
        if (userId != null) {
            invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*:u" + userId + ":*");
        }

        // 图片总数缓存也需要清除（保持原有逻辑）
        invalidateByPattern(RedisKeyConstants.COUNT_PREFIX + "*");

        log.info("图片上传后已清除相关缓存 - 用户ID={}, 包含所有主页缓存", userId);
    }

    /**
     * 图片编辑后清除相关缓存
     */
    public void invalidateAfterPictureEdit(Long pictureId, Long oldCategoryId, Long newCategoryId,
                                           List<Long> oldTagIds, List<Long> newTagIds) {
        // 清除分类相关缓存
        if (oldCategoryId != null) {
            invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*:c" + oldCategoryId + ":*");
        }

        if (newCategoryId != null && !newCategoryId.equals(oldCategoryId)) {
            invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*:c" + newCategoryId + ":*");
        }

        // 清除标签相关缓存
        Set<Long> allTagIds = new HashSet<>();
        if (CollUtil.isNotEmpty(oldTagIds)) {
            allTagIds.addAll(oldTagIds);
        }
        if (CollUtil.isNotEmpty(newTagIds)) {
            allTagIds.addAll(newTagIds);
        }

        for (Long tagId : allTagIds) {
            invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*:t*" + tagId + "*:*");
        }

        // 清除首页缓存（所有排序方式）
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "initial:*");

        // 清除所有排序相关的缓存
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*newest*");
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*popular*");
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*mostViewed*");
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*mostLiked*");
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*mostCollected*");

        // 清除图片总数相关缓存
        invalidateByPattern(RedisKeyConstants.COUNT_PREFIX + "*");

        log.info("图片编辑后已清除相关缓存: pictureId={}, 包含首页和排序缓存", pictureId);
    }
    
    /**
     * 图片审核状态变更后清除缓存
     */
    public void invalidateAfterReviewStatusChange() {
        // 审核状态变更会影响所有瀑布流缓存
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*");
        invalidateByPattern(RedisKeyConstants.COUNT_PREFIX + "*");
        log.info("图片审核状态变更后已清除所有瀑布流缓存");
    }
    
    /**
     * 图片浏览量变更后清除缓存（带阈值控制）
     */
    public void invalidateAfterViewCountIncrement(Long pictureId) {
        AtomicInteger counter = viewCountThresholds.computeIfAbsent(pictureId, k -> new AtomicInteger(0));
        
        // 当计数达到阈值时才执行缓存清除
        if (counter.incrementAndGet() >= VIEW_COUNT_THRESHOLD) {
            counter.set(0);
            invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*:popular:*");
            invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*:mostViewed:*");
            log.info("图片浏览量增加达到阈值，已清除排序相关缓存: pictureId={}", pictureId);
        }
    }
    
    /**
     * 图片点赞数变更后清除缓存
     */
    public void invalidateAfterLikeCountChange(Long pictureId) {
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*:mostLiked:*");
        log.info("图片点赞数变更后已清除排序相关缓存: pictureId={}", pictureId);
    }
    
    /**
     * 图片收藏数变更后清除缓存
     */
    public void invalidateAfterCollectionCountChange(Long pictureId) {
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*:mostCollected:*");
        log.info("图片收藏数变更后已清除排序相关缓存: pictureId={}", pictureId);
    }
    
    /**
     * 批量操作后清除所有缓存
     */
    public void invalidateAllCaches() {
        invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*");
        invalidateByPattern(RedisKeyConstants.COUNT_PREFIX + "*");
        log.info("已清除所有图片相关缓存");
    }
    
    /**
     * 使用scan命令高效清除匹配模式的缓存键
     * 相比keys命令更适合生产环境
     */
    private void invalidateByPattern(String pattern) {
        try {
            Set<String> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
            
            Cursor<String> cursor = redisTemplate.scan(options);
            while (cursor.hasNext()) {
                keys.add(cursor.next());
                
                // 当收集到一定数量的键时批量删除
                if (keys.size() >= 100) {
                    if (!keys.isEmpty()) {
                        redisTemplate.delete(keys);
                    }
                    keys.clear();
                }
            }
            
            // 删除剩余的键
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("已清除{}个缓存键", keys.size());
            }
            
            cursor.close();
        } catch (Exception e) {
            log.error("清除缓存键失败: pattern={}, error={}", pattern, e.getMessage(), e);
        }
    }

    /**
     * 清除所有与计数相关的缓存（用于定时任务后的缓存刷新）
     */
    public void invalidateCountRelatedCaches() {
        try {
            // 清除所有基于计数排序的缓存
            invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*:popular:*");
            invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*:mostViewed:*");
            invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*:mostLiked:*");
            invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*:mostCollected:*");
            log.info("已清除所有与计数相关的缓存");
        } catch (Exception e) {
            log.error("清除计数相关缓存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 图片删除后清除相关缓存
     * @param pictureId 图片ID
     */
    public void invalidateAfterPictureDelete(Long pictureId) {
        try {

            // 清除瀑布流相关缓存
            invalidateByPattern(RedisKeyConstants.WATERFALL_PREFIX + "*");

            // 清除图片计数相关缓存
            invalidateByPattern(RedisKeyConstants.COUNT_PREFIX + "*");

            log.info("图片删除后已清除相关缓存: pictureId={}", pictureId);
        } catch (Exception e) {
            log.error("清除图片删除相关缓存失败: pictureId={}, error={}", pictureId, e.getMessage(), e);
        }
    }
}