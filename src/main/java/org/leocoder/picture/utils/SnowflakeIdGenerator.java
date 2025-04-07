package org.leocoder.picture.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 15:10
 * @description :  雪花算法ID生成器
 */

@Slf4j
public class SnowflakeIdGenerator {

    // 开始时间戳 (2023-01-01)
    private final long startEpoch = 1672531200000L;
    
    // 机器ID所占位数
    private final long workerIdBits = 5L;
    
    // 数据中心ID所占位数
    private final long dataCenterIdBits = 5L;
    
    // 支持的最大机器ID，结果是31
    private final long maxWorkerId = ~(-1L << workerIdBits);
    
    // 支持的最大数据中心ID，结果是31
    private final long maxDataCenterId = ~(-1L << dataCenterIdBits);
    
    // 序列号所占位数
    private final long sequenceBits = 12L;
    
    // 机器ID向左移12位
    private final long workerIdShift = sequenceBits;
    
    // 数据中心ID向左移17位
    private final long dataCenterIdShift = sequenceBits + workerIdBits;
    
    // 时间戳向左移22位
    private final long timestampLeftShift = sequenceBits + workerIdBits + dataCenterIdBits;
    
    // 生成序列的掩码，这里为4095
    private final long sequenceMask = ~(-1L << sequenceBits);
    
    // 工作机器ID(0~31)
    private long workerId;
    
    // 数据中心ID(0~31)
    private long dataCenterId;
    
    // 毫秒内序列(0~4095)
    private long sequence = 0L;
    
    // 上次生成ID的时间戳
    private long lastTimestamp = -1L;

    /**
     * 构造函数
     * @param workerId 工作ID (0~31)
     * @param dataCenterId 数据中心ID (0~31)
     */
    public SnowflakeIdGenerator(long workerId, long dataCenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("Worker ID can't be greater than " + maxWorkerId + " or less than 0");
        }
        if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
            throw new IllegalArgumentException("DataCenter ID can't be greater than " + maxDataCenterId + " or less than 0");
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
        log.info("SnowflakeIdGenerator initialized with workerId: {}, dataCenterId: {}", workerId, dataCenterId);
    }

    /**
     * 生成雪花算法ID
     * @return 返回ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id for " 
                    + (lastTimestamp - timestamp) + " milliseconds");
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒，获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 时间戳改变，毫秒内序列重置
            sequence = 0L;
        }

        // 上次生成ID的时间戳
        lastTimestamp = timestamp;

        // 组合ID (时间戳部分 | 数据中心部分 | 机器标识部分 | 序列号部分)
        return ((timestamp - startEpoch) << timestampLeftShift) |
                (dataCenterId << dataCenterIdShift) |
                (workerId << workerIdShift) |
                sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 当前时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回当前时间的毫秒数
     * @return 当前时间的毫秒数
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }
}