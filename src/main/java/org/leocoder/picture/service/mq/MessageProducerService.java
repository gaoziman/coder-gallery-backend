package org.leocoder.picture.service.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.leocoder.picture.domain.message.PictureReactionMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-21 22:49
 * @description : RocketMQ消息生产者服务
 *  负责发送各类业务消息到消息队列
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProducerService {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 图片反应主题
     */
    private static final String PICTURE_REACTION_TOPIC = "PICTURE_REACTION_TOPIC";

    /**
     * 发送图片反应消息
     *
     * @param message 图片反应消息对象
     * @return 是否成功发送
     */
    public boolean sendPictureReactionMessage(PictureReactionMessage message) {
        try {
            log.info("发送图片反应消息: pictureId={}, reactionType={}, operationType={}, userId={}",
                    message.getPictureId(), message.getReactionType(), message.getOperationType(), message.getUserId());

            rocketMQTemplate.syncSend(PICTURE_REACTION_TOPIC,
                    MessageBuilder.withPayload(message).build());

            log.info("图片反应消息发送成功");
            return true;
        } catch (Exception e) {
            log.error("图片反应消息发送失败: {}", e.getMessage(), e);
            return false;
        }
    }
}