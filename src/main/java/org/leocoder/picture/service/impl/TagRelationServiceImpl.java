package org.leocoder.picture.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.domain.pojo.TagRelation;
import org.leocoder.picture.mapper.TagRelationMapper;
import org.leocoder.picture.service.TagRelationService;
import org.leocoder.picture.service.TagService;
import org.leocoder.picture.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author : 程序员Leo
 * @date  2025-04-09 10:24
 * @version 1.0
 * @description : 标签关系服务实现类
 */
@Slf4j
@Service
public class TagRelationServiceImpl implements TagRelationService {

    private final TagRelationMapper tagRelationMapper;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private TagService tagService;

    public TagRelationServiceImpl(TagRelationMapper tagRelationMapper,
                                  SnowflakeIdGenerator idGenerator) {
        this.tagRelationMapper = tagRelationMapper;
        this.snowflakeIdGenerator = idGenerator;
    }

    @Autowired
    public void setTagService(TagService tagService) {
        this.tagService = tagService;
    }

    /**
     * 创建标签关系
     *
     * @param tagId 标签ID
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean createTagRelation(Long tagId, String contentType, Long contentId) {
        // 检查关系是否已存在
        if (checkRelationExists(tagId, contentType, contentId)) {
            return true;
        }

        // 创建关系
        TagRelation relation = TagRelation.builder()
                .id(snowflakeIdGenerator.nextId())
                .tagId(tagId)
                .contentType(contentType)
                .contentId(contentId)
                .createTime(LocalDateTime.now())
                .createUser(StpUtil.getLoginIdAsLong())
                .updateTime(LocalDateTime.now())
                .updateUser(StpUtil.getLoginIdAsLong())
                .isDeleted(0)
                .build();

        tagRelationMapper.insertWithId(relation);
        return true;
    }

    /**
     * 批量创建标签关系
     *
     * @param tagIds 标签ID列表
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchCreateTagRelations(List<Long> tagIds, String contentType, Long contentId) {
        if (CollUtil.isEmpty(tagIds)) {
            return false;
        }

        // 过滤出尚未关联的标签
        List<Long> existingTagIds = tagRelationMapper.selectTagIdsByContent(contentType, contentId);
        List<Long> newTagIds = tagIds.stream()
                .filter(tagId -> !existingTagIds.contains(tagId))
                .collect(Collectors.toList());

        if (newTagIds.isEmpty()) {
            return true;
        }

        // 批量创建关系
        for (Long tagId : newTagIds) {
            TagRelation relation = TagRelation.builder()
                    .id(snowflakeIdGenerator.nextId())
                    .tagId(tagId)
                    .contentType(contentType)
                    .contentId(contentId)
                    .createTime(LocalDateTime.now())
                    .createUser(StpUtil.getLoginIdAsLong())
                    .updateTime(LocalDateTime.now())
                    .updateUser(StpUtil.getLoginIdAsLong())
                    .isDeleted(0)
                    .build();

            tagRelationMapper.insertWithId(relation);

            // 更新标签引用次数
            tagService.updateTagReferenceCount(tagId, 1);
        }

        log.info("批量创建标签关系成功: tagIds数量={}, contentType={}, contentId={}",
                newTagIds.size(), contentType, contentId);

        return true;
    }


    /**
     * 删除标签关系
     *
     * @param tagId 标签ID
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTagRelation(Long tagId, String contentType, Long contentId) {
        // 查询关系是否存在
        TagRelation relation = tagRelationMapper.selectByTagAndContent(tagId, contentType, contentId);
        if (relation == null || relation.getIsDeleted() == 1) {
            return true;
        }

        // 删除关系
        tagRelationMapper.deleteById(relation.getId());

        // 更新标签引用次数
        tagService.updateTagReferenceCount(tagId, -1);

        log.info("删除标签关系成功: tagId={}, contentType={}, contentId={}",
                tagId, contentType, contentId);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteAllRelationsByContent(String contentType, Long contentId) {
        // 获取内容关联的所有标签ID
        List<Long> tagIds = tagRelationMapper.selectTagIdsByContent(contentType, contentId);
        if (tagIds.isEmpty()) {
            return true;
        }

        // 删除所有关系
        tagRelationMapper.deleteByContent(contentType, contentId);

        // 更新标签引用次数
        for (Long tagId : tagIds) {
            tagService.updateTagReferenceCount(tagId, -1);
        }

        log.info("删除内容的所有标签关系成功: contentType={}, contentId={}, 标签数量={}",
                contentType, contentId, tagIds.size());

        return true;
    }


    /**
     * 删除指定标签的所有关系
     *
     * @param tagId 标签ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteAllRelationsByTag(Long tagId) {
        // 查询标签关联的内容数量
        int count = tagRelationMapper.countByTagId(tagId, null);
        if (count == 0) {
            return true;
        }

        // 删除所有关系
        tagRelationMapper.deleteByTagId(tagId);

        // 更新标签引用次数
        tagService.updateTagReferenceCount(tagId, -count);

        log.info("删除标签的所有关系成功: tagId={}, 关系数量={}", tagId, count);

        return true;
    }


    /**
     * 批量删除标签的所有关系
     *
     * @param tagIds 标签ID列表
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDeleteAllRelationsByTags(List<Long> tagIds) {
        if (CollUtil.isEmpty(tagIds)) {
            return false;
        }

        // 保存每个标签关联的内容数量，用于更新引用计数
        Map<Long, Integer> tagRelationCounts = new HashMap<>();
        for (Long tagId : tagIds) {
            int count = tagRelationMapper.countByTagId(tagId, null);
            if (count > 0) {
                tagRelationCounts.put(tagId, count);
            }
        }

        if (tagRelationCounts.isEmpty()) {
            return true;
        }

        // 批量删除关系
        tagRelationMapper.batchDeleteByTagIds(tagIds);

        // 更新标签引用次数
        for (Map.Entry<Long, Integer> entry : tagRelationCounts.entrySet()) {
            tagService.updateTagReferenceCount(entry.getKey(), -entry.getValue());
        }

        log.info("批量删除标签关系成功: 标签数量={}", tagIds.size());

        return true;
    }


    /**
     * 更新内容的标签关系
     *
     * @param tagIds 标签ID列表
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateContentTags(List<Long> tagIds, String contentType, Long contentId) {
        // 获取内容当前关联的标签ID列表
        List<Long> currentTagIds = tagRelationMapper.selectTagIdsByContent(contentType, contentId);

        // 找出需要新增的标签
        List<Long> tagsToAdd = new ArrayList<>();
        if (CollUtil.isNotEmpty(tagIds)) {
            tagsToAdd = tagIds.stream()
                    .filter(tagId -> !currentTagIds.contains(tagId))
                    .collect(Collectors.toList());
        }

        // 找出需要删除的标签
        List<Long> tagsToRemove = new ArrayList<>();
        if (CollUtil.isNotEmpty(currentTagIds)) {
            tagsToRemove = currentTagIds.stream()
                    .filter(tagId -> CollUtil.isEmpty(tagIds) || !tagIds.contains(tagId))
                    .collect(Collectors.toList());
        }

        // 添加新标签关系
        if (CollUtil.isNotEmpty(tagsToAdd)) {
            batchCreateTagRelations(tagsToAdd, contentType, contentId);
        }

        // 删除旧标签关系
        for (Long tagId : tagsToRemove) {
            deleteTagRelation(tagId, contentType, contentId);
        }

        log.info("更新内容标签关系成功: contentType={}, contentId={}, 新增标签数={}, 删除标签数={}",
                contentType, contentId, tagsToAdd.size(), tagsToRemove.size());

        return true;
    }


    /**
     * 获取内容关联的标签ID列表
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 标签ID列表
     */
    @Override
    public List<Long> getTagIdsByContent(String contentType, Long contentId) {
        return tagRelationMapper.selectTagIdsByContent(contentType, contentId);
    }


    /**
     * 查询标签下有多少内容
     *
     * @param tagId 标签ID
     * @param contentType 内容类型（可选）
     * @return 内容数量
     */
    @Override
    public Integer countContentsByTag(Long tagId, String contentType) {
        return tagRelationMapper.countByTagId(tagId, contentType);
    }


    /**
     * 检查标签关系是否存在
     *
     * @param tagId 标签ID
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否存在
     */
    @Override
    public Boolean checkRelationExists(Long tagId, String contentType, Long contentId) {
        TagRelation relation = tagRelationMapper.selectByTagAndContent(tagId, contentType, contentId);
        return relation != null && relation.getIsDeleted() == 0;
    }


    /**
     * 删除内容的所有标签关联关系，但不更新引用计数
     *
     * @param contentType 内容类型（如"picture"）
     * @param contentId 内容ID
     * @return 是否成功
     */
    @Override
    public boolean deleteAllTagRelations(String contentType, Long contentId) {
        // 参数校验
        if (StrUtil.isBlank(contentType) || contentId == null || contentId <= 0) {
            log.error("删除标签关联参数无效: contentType={}, contentId={}", contentType, contentId);
            return false;
        }

        try {
            // 直接通过条件删除所有匹配的记录
            int result = tagRelationMapper.deleteByContentTypeAndContentId(contentType, contentId);
            log.info("已删除内容的所有标签关联: contentType={}, contentId={}, 删除记录数={}",
                    contentType, contentId, result);
            // 返回true表示操作成功，即使没有记录被删除
            return result >= 0;
        } catch (Exception e) {
            log.error("删除内容标签关联异常: contentType={}, contentId={}", contentType, contentId, e);
            return false;
        }
    }
}