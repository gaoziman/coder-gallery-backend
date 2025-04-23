package org.leocoder.picture.service.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.leocoder.picture.cache.PictureCacheManager;
import org.leocoder.picture.constants.RedisConstants;
import org.leocoder.picture.domain.message.PictureReactionMessage;
import org.leocoder.picture.mapper.PictureMapper;
import org.springframework.stereotype.Service;

/**
 * @author : 程序员Leo
 * @version 2.0
 * @date 2025-04-24 14:30
 * @description : 图片反应消息消费者（增强版，支持缓存一致性）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${rocketmq.consumer.picture-reaction.topic}",
        consumerGroup = "${rocketmq.consumer.picture-reaction.group}")
public class PictureReactionConsumer implements RocketMQListener<PictureReactionMessage> {

    private final PictureMapper pictureMapper;
    private final PictureCacheManager pictureCacheManager; // 添加缓存管理器依赖

    /**
     * 处理图片反应消息
     *
     * @param message 图片反应消息
     */
    @Override
    public void onMessage(PictureReactionMessage message) {
        try {
            log.info("接收到图片反应消息: pictureId={}, reactionType={}, operationType={}, userId={}",
                    message.getPictureId(), message.getReactionType(), message.getOperationType(), message.getUserId());

            // 处理消息
            processReactionMessage(message);

            // 根据反应类型执行相应的缓存失效策略
            invalidateRelatedCaches(message);

            log.info("图片反应消息处理成功");
        } catch (Exception e) {
            log.error("图片反应消息处理失败: {}", e.getMessage(), e);
            // 消费失败，消息会重试
            throw e;
        }
    }

    /**
     * 处理图片反应消息
     *
     * @param message 图片反应消息
     */
    private void processReactionMessage(PictureReactionMessage message) {
        Long pictureId = message.getPictureId();
        String reactionType = message.getReactionType();
        String operationType = message.getOperationType();

        // 根据操作类型和反应类型更新图片计数
        if ("add".equals(operationType)) {
            // 增加计数
            if (RedisConstants.REACTION_LIKE.equals(reactionType)) {
                pictureMapper.incrementLikeCount(pictureId);
                log.info("增加图片点赞计数: pictureId={}", pictureId);
            } else if (RedisConstants.REACTION_FAVORITE.equals(reactionType)) {
                pictureMapper.incrementCollectionCount(pictureId);
                log.info("增加图片收藏计数: pictureId={}", pictureId);
            } else if (RedisConstants.REACTION_VIEW.equals(reactionType)) {
                pictureMapper.incrementViewCount(pictureId);
                log.info("增加图片浏览计数: pictureId={}", pictureId);
            }
        } else if ("remove".equals(operationType)) {
            // 减少计数
            if (RedisConstants.REACTION_LIKE.equals(reactionType)) {
                pictureMapper.decrementLikeCount(pictureId);
                log.info("减少图片点赞计数: pictureId={}", pictureId);
            } else if (RedisConstants.REACTION_FAVORITE.equals(reactionType)) {
                pictureMapper.decrementCollectionCount(pictureId);
                log.info("减少图片收藏计数: pictureId={}", pictureId);
            }
        }
    }

    /**
     * 根据反应类型清除相关缓存
     *
     * @param message 图片反应消息
     */
    private void invalidateRelatedCaches(PictureReactionMessage message) {
        Long pictureId = message.getPictureId();
        String reactionType = message.getReactionType();

        switch (reactionType) {
            case RedisConstants.REACTION_VIEW:
                // 浏览量变更，影响"热门"和"浏览最多"排序的缓存
                pictureCacheManager.invalidateAfterViewCountIncrement(pictureId);
                break;

            case RedisConstants.REACTION_LIKE:
                // 点赞变更，影响"点赞最多"排序的缓存
                pictureCacheManager.invalidateAfterLikeCountChange(pictureId);
                break;

            case RedisConstants.REACTION_FAVORITE:
                // 收藏变更，影响"收藏最多"排序的缓存
                pictureCacheManager.invalidateAfterCollectionCountChange(pictureId);
                break;

            default:
                log.warn("未知的反应类型: {}, 不执行缓存清除", reactionType);
        }
    }
}