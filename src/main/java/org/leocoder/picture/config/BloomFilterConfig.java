package org.leocoder.picture.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.mapper.UserMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 14:35
 * @description : 布隆过滤器配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BloomFilterConfig {

    private final UserMapper userMapper;

    private BloomFilter<String> accountBloomFilter;

    /**
     * 初始化布隆过滤器
     */
    @PostConstruct
    public void initBloomFilter() {
        log.info("开始初始化用户账号布隆过滤器...");
        // 查询所有用户账号
        List<User> userList = userMapper.selectAllAccounts();
        // 预计插入量，默认误判率0.001
        accountBloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                // 容量预留两倍
                userList.size() * 2,
                0.001);

        // 将所有账号放入布隆过滤器
        for (User user : userList) {
            accountBloomFilter.put(user.getAccount());
        }
        log.info("用户账号布隆过滤器初始化完成, 总数: {}", userList.size());
    }

    /**
     * 每天凌晨2点重建布隆过滤器
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void rebuildBloomFilter() {
        log.info("开始重建用户账号布隆过滤器...");
        initBloomFilter();
    }

    /**
     * 将布隆过滤器暴露为Bean
     */
    @Bean
    public BloomFilter<String> accountBloomFilter() {
        return this.accountBloomFilter;
    }

    /**
     * 添加新账号到布隆过滤器
     */
    public void addAccount(String account) {
        this.accountBloomFilter.put(account);
    }

    /**
     * 检查账号是否可能存在
     */
    public boolean mightContain(String account) {
        return this.accountBloomFilter.mightContain(account);
    }
}