package org.leocoder.picture.config;

import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.InetAddress;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 15:59
 * @description : ID生成器配置类
 */
@Configuration
@Slf4j
public class IdGeneratorConfig {

    /**
     * 注册短ID生成器（作为主要ID生成器）
     */
    @Bean
    @Primary
    public SnowflakeIdGenerator shortIdGenerator(
            @Value("${shortid.worker-id:1}") Integer configuredWorkerId,
            @Value("${shortid.data-center-id:1}") Integer configuredDataCenterId) {

        try {
            InetAddress address = InetAddress.getLocalHost();
            String hostAddress = address.getHostAddress();
            String[] parts = hostAddress.split("\\.");

            // 短ID生成器的workerId范围是0-15，dataCenterId范围是0-7
            int workerId = (configuredWorkerId == 1) ?
                    Math.abs(Integer.parseInt(parts[2])) % 16 : configuredWorkerId;
            int dataCenterId = (configuredDataCenterId == 1) ?
                    Math.abs(Integer.parseInt(parts[3])) % 8 : configuredDataCenterId;

            log.info("Configured short ID generator with workerId={}, dataCenterId={} (IP-based: {})",
                    workerId, dataCenterId, configuredWorkerId == 1 || configuredDataCenterId == 1);

            return new SnowflakeIdGenerator(workerId, dataCenterId);
        } catch (Exception e) {
            log.warn("Failed to auto-configure short ID generator, using configured values: workerId={}, dataCenterId={}",
                    configuredWorkerId, configuredDataCenterId, e);
            return new SnowflakeIdGenerator(configuredWorkerId, configuredDataCenterId);
        }
    }
}