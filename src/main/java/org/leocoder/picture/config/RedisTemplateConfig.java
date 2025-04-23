package org.leocoder.picture.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("初始化增强版RedisTemplate配置...");

        // 创建RedisTemplate实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 配置高级序列化器
        Jackson2JsonRedisSerializer<Object> valueSerializer = configureValueSerializer();

        // 配置键的序列化方式
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        // 设置RedisTemplate的序列化器
        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);

        // 设置Hash操作的序列化器
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);

        // 设置默认的序列化器
        template.setDefaultSerializer(valueSerializer);

        // 初始化Template
        template.afterPropertiesSet();

        log.info("RedisTemplate配置完成 - 使用Jackson2JsonRedisSerializer进行对象序列化");
        return template;
    }

    /**
     * 配置高级值序列化器
     */
    private Jackson2JsonRedisSerializer<Object> configureValueSerializer() {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();

        // 设置对象的所有属性都可视化
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 启用默认类型信息，确保反序列化时能正确恢复对象类型
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);

        // 添加Java 8日期时间模块支持
        mapper.registerModule(new JavaTimeModule());

        // 禁用将日期写为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 配置反序列化选项 - 忽略未知属性，避免因为类结构变化导致的反序列化失败
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 允许空对象序列化
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 设置ObjectMapper到序列化器
        serializer.setObjectMapper(mapper);

        return serializer;
    }
}