package org.leocoder.picture.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.cache.PictureCacheManager;
import org.leocoder.picture.constants.RedisConstants;
import org.leocoder.picture.domain.pojo.Picture;
import org.leocoder.picture.mapper.PictureMapper;
import org.leocoder.picture.mapper.UserReactionMapper;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-21 22:49
 * @description : 图片计数校准任务
 *  定期同步用户反应表和图片表中的计数数据，保证数据一致性
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class PictureCountReconciliationTask {

    private final PictureMapper pictureMapper;

    private final UserReactionMapper userReactionMapper;

    private final PictureCacheManager pictureCacheManager;
    
    /**
     * 每天凌晨2点执行计数校准任务
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void reconcilePictureCounts() {
        log.info("开始执行图片计数校准任务");
        
        int batchSize = 100;
        long lastId = 0;
        int totalProcessed = 0;
        int totalUpdated = 0;
        
        try {
            while (true) {
                // 分批获取图片数据
                List<Picture> pictures = pictureMapper.selectBatchPictures(lastId, batchSize);
                if (pictures.isEmpty()) {
                    break;
                }
                
                for (Picture picture : pictures) {
                    try {
                        // 从用户反应表获取准确的点赞数和收藏数
                        long actualLikeCount = userReactionMapper.countReactionsByTarget(
                                RedisConstants.TARGET_PICTURE, picture.getId(), RedisConstants.REACTION_LIKE);
                        
                        long actualFavoriteCount = userReactionMapper.countReactionsByTarget(
                                RedisConstants.TARGET_PICTURE, picture.getId(), RedisConstants.REACTION_FAVORITE);
                        
                        // 判断是否需要更新
                        boolean needUpdate = false;
                        
                        // 点赞数不一致
                        if (picture.getLikeCount() == null || actualLikeCount != picture.getLikeCount()) {
                            needUpdate = true;
                        }
                        
                        // 收藏数不一致
                        if (picture.getCollectionCount() == null || actualFavoriteCount != picture.getCollectionCount()) {
                            needUpdate = true;
                        }
                        
                        // 需要更新
                        if (needUpdate) {
                            pictureMapper.updateCounters(picture.getId(), actualLikeCount, actualFavoriteCount);
                            totalUpdated++;
                            
                            log.info("校准图片计数: id={}, 点赞从{}调整到{}, 收藏从{}调整到{}",
                                    picture.getId(), picture.getLikeCount(), actualLikeCount,
                                    picture.getCollectionCount(), actualFavoriteCount);
                        }
                    } catch (Exception e) {
                        log.error("校准图片[{}]计数失败: {}", picture.getId(), e.getMessage(), e);
                    }
                    
                    // 更新最后处理的ID
                    lastId = picture.getId();
                    totalProcessed++;
                }
                
                log.info("批次处理完成，已处理{}张图片，更新{}张图片", pictures.size(), totalUpdated);
            }

            if (totalUpdated > 0) {
                log.info("由于校准了{}张图片的计数，正在清除相关缓存...", totalUpdated);
                pictureCacheManager.invalidateCountRelatedCaches();
            }

            log.info("图片计数校准任务完成，共处理{}张图片，更新{}张图片", totalProcessed, totalUpdated);
        } catch (Exception e) {
            log.error("图片计数校准任务执行异常: {}", e.getMessage(), e);
        }
    }
}