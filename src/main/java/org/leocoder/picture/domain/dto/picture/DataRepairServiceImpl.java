package org.leocoder.picture.domain.dto.picture;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.domain.pojo.Tag;
import org.leocoder.picture.domain.vo.tag.TagVO;
import org.leocoder.picture.mapper.TagMapper;
import org.leocoder.picture.mapper.TagRelationMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-27
 * @description : 数据修复服务 - 负责定期修复数据不一致问题
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataRepairServiceImpl {

    private final TagMapper tagMapper;
    private final TagRelationMapper tagRelationMapper;

    /**
     * 修复所有标签的引用计数
     * 每天凌晨2点执行一次
     */
    @Scheduled(cron = "${system.maintenance.tag-repair-cron:0 0 2 * * ?}")
    @Transactional(rollbackFor = Exception.class)
    public void repairAllTagReferenceCount() {
        log.info("开始执行标签引用计数修复任务");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取所有非删除状态的标签
            List<TagVO> allTags = tagMapper.selectTagList();
            if (allTags.isEmpty()) {
                log.info("没有需要修复的标签");
                return;
            }

            log.info("共发现{}个标签需要检查", allTags.size());

            // 2. 批量获取所有标签的实际引用次数
            List<Map<String, Object>> actualCounts = tagRelationMapper.countAllTagReferences();

            // 3. 构建标签ID到实际引用次数的映射
            Map<Long, Integer> tagReferenceMap = new HashMap<>();
            for (Map<String, Object> count : actualCounts) {
                Long tagId = (Long) count.get("tag_id");
                Integer refCount = ((Number) count.get("ref_count")).intValue();
                tagReferenceMap.put(tagId, refCount);
            }

            // 4. 检查并修复每个标签的引用计数
            int fixedCount = 0;
            int errorCount = 0;

            for (TagVO tag : allTags) {
                try {
                    // 获取标签当前的引用计数
                    Integer currentCount = tag.getReferenceCount();
                    // 获取实际引用计数（不存在则为0）
                    Integer actualCount = tagReferenceMap.getOrDefault(tag.getId(), 0);

                    // 如果计数不一致，则进行修复
                    if (!actualCount.equals(currentCount)) {
                        // 计算需要增加或减少的差值
                        int incrementValue = actualCount - currentCount;

                        // 更新标签的引用计数（使用增量方式）
                        tagMapper.updateReferenceCount(tag.getId(), incrementValue);

                        log.info("修复标签[{}]的引用计数: {} -> {} (增量: {})",
                                tag.getName(), currentCount, actualCount, incrementValue);
                        fixedCount++;
                    }
                } catch (Exception e) {
                    log.error("修复标签[{}]引用计数时出错", tag.getId(), e);
                    errorCount++;
                }
            }

            long endTime = System.currentTimeMillis();
            log.info("标签引用计数修复任务完成: 总标签数={}, 已修复={}, 错误={}, 耗时={}ms",
                    allTags.size(), fixedCount, errorCount, (endTime - startTime));
        } catch (Exception e) {
            log.error("执行标签引用计数修复任务失败", e);
        }
    }

    /**
     * 修复特定标签的引用计数
     * 可由管理员手动触发
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean repairTagReferenceCount(Long tagId) {
        try {
            // 查询标签是否存在
            Tag tag = tagMapper.selectById(tagId);
            if (tag == null || tag.getIsDeleted() == 1) {
                log.warn("标签不存在或已删除: {}", tagId);
                return false;
            }

            // 获取标签当前引用计数
            Integer currentCount = tag.getReferenceCount();

            // 计算标签实际引用次数
            Integer actualCount = tagRelationMapper.countByTagId(tagId, null);

            // 如果计数不一致，则进行修复
            if (!actualCount.equals(currentCount)) {
                // 计算需要增加或减少的差值
                int incrementValue = actualCount - currentCount;

                // 更新标签的引用计数（使用增量方式）
                tagMapper.updateReferenceCount(tagId, incrementValue);

                log.info("手动修复标签[{}]的引用计数: {} -> {} (增量: {})",
                        tag.getName(), currentCount, actualCount, incrementValue);
                return true;
            } else {
                log.info("标签[{}]引用计数正确，无需修复", tag.getName());
                return true;
            }
        } catch (Exception e) {
            log.error("手动修复标签[{}]引用计数时出错", tagId, e);
            return false;
        }
    }
}