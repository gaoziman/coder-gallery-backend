package org.leocoder.picture.config;

import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 15:59
 * @description :
 */
@Configuration
@Slf4j
public class IdGeneratorConfig {

    /**
     * 注册雪花算法ID生成器
     * <p>
     * workerId和dataCenterId可以根据实际情况进行配置
     */
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(
            @Value("${snowflake.worker-id:1}") Integer configuredWorkerId,
            @Value("${snowflake.data-center-id:1}") Integer configuredDataCenterId) {

        // 尝试基于IP自动配置
        try {
            InetAddress address = InetAddress.getLocalHost();
            String hostAddress = address.getHostAddress();
            String[] parts = hostAddress.split("\\.");

            // 只有在配置为默认值1时才使用自动配置
            int workerId = (configuredWorkerId == 1) ?
                    Math.abs(Integer.parseInt(parts[2])) % 32 : configuredWorkerId;
            int dataCenterId = (configuredDataCenterId == 1) ?
                    Math.abs(Integer.parseInt(parts[3])) % 32 : configuredDataCenterId;

            log.info("Configured snowflake with workerId={}, dataCenterId={} (IP-based: {})",
                    workerId, dataCenterId, configuredWorkerId == 1 || configuredDataCenterId == 1);

            return new SnowflakeIdGenerator(workerId, dataCenterId);
        } catch (Exception e) {
            log.warn("Failed to auto-configure snowflake, using configured values: workerId={}, dataCenterId={}",
                    configuredWorkerId, configuredDataCenterId, e);
            return new SnowflakeIdGenerator(configuredWorkerId, configuredDataCenterId);
        }
    }
}