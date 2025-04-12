package org.leocoder.picture.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.domain.pojo.CategoryRelation;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.mapper.CategoryRelationMapper;
import org.leocoder.picture.service.CategoryRelationService;
import org.leocoder.picture.utils.SnowflakeIdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : 程序员Leo
 * @date  2025-04-08 13:59
 * @version 1.0
 * @description : 分类关系服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryRelationServiceImpl implements CategoryRelationService {

    private final CategoryRelationMapper categoryRelationMapper;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 创建分类关系
     *
     * @param categoryId 分类ID
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean createCategoryRelation(Long categoryId, String contentType, Long contentId) {
        // 1. 参数校验
        if (ObjectUtil.isNull(categoryId) || ObjectUtil.isNull(contentType) || ObjectUtil.isNull(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 检查是否已存在相同的关系
        CategoryRelation existRelation = categoryRelationMapper.selectByCondition(categoryId, contentType, contentId);
        if (ObjectUtil.isNotNull(existRelation) && !Boolean.TRUE.equals(existRelation.getIsDeleted())) {
            // 已存在且未删除，直接返回成功
            return true;
        }

        // 3. 构建关系对象
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();
        Long relationId = snowflakeIdGenerator.nextId();

        CategoryRelation relation = CategoryRelation.builder()
                .id(relationId)
                .categoryId(categoryId)
                .contentType(contentType)
                .contentId(contentId)
                .createTime(now)
                .createUser(userId)
                .updateTime(now)
                .updateUser(userId)
                .isDeleted(0)
                .build();

        // 4. 插入数据库
        int result = categoryRelationMapper.insertWithId(relation);

        return result > 0;
    }

    /**
     * 批量创建分类关系
     *
     * @param categoryIds 分类ID列表
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchCreateCategoryRelations(List<Long> categoryIds, String contentType, Long contentId) {
        // 1. 参数校验
        if (CollUtil.isEmpty(categoryIds) || ObjectUtil.isNull(contentType) || ObjectUtil.isNull(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 获取当前用户ID和时间
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        // 3. 构建关系对象列表
        List<CategoryRelation> relationList = new ArrayList<>();

        for (Long categoryId : categoryIds) {
            // 检查是否已存在相同的关系
            CategoryRelation existRelation = categoryRelationMapper.selectByCondition(categoryId, contentType, contentId);
            if (ObjectUtil.isNotNull(existRelation)) {
                // 已存在且未删除，跳过
                continue;
            }

            Long relationId = snowflakeIdGenerator.nextId();

            CategoryRelation relation = CategoryRelation.builder()
                    .id(relationId)
                    .categoryId(categoryId)
                    .contentType(contentType)
                    .contentId(contentId)
                    .createTime(now)
                    .createUser(userId)
                    .updateTime(now)
                    .updateUser(userId)
                    .isDeleted(0)
                    .build();

            relationList.add(relation);
        }

        // 如果没有需要创建的关系，直接返回成功
        if (relationList.isEmpty()) {
            return true;
        }

        // 4. 批量插入数据库
        int result = categoryRelationMapper.batchInsert(relationList);

        return result > 0;
    }

    /**
     * 删除分类关系
     *
     * @param categoryId 分类ID
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCategoryRelation(Long categoryId, String contentType, Long contentId) {
        // 1. 参数校验
        if (ObjectUtil.isNull(categoryId) || ObjectUtil.isNull(contentType) || ObjectUtil.isNull(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 查询关系是否存在
        CategoryRelation relation = categoryRelationMapper.selectByCondition(categoryId, contentType, contentId);
        if (ObjectUtil.isNull(relation)) {
            // 关系不存在或已删除，直接返回成功
            return true;
        }

        // 3. 逻辑删除关系
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        int result = categoryRelationMapper.logicDeleteRelation(relation.getId(), now, userId);

        return result > 0;
    }

    /**
     * 删除指定内容的所有分类关系
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteAllRelationsByContent(String contentType, Long contentId) {
        // 1. 参数校验
        if (ObjectUtil.isNull(contentType) || ObjectUtil.isNull(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 删除关系
        int result = categoryRelationMapper.deleteByContent(contentType, contentId);

        // 即使没有删除任何记录，也认为操作成功
        return result >= 0;
    }

    /**
     * 删除指定分类的所有关系
     *
     * @param categoryId 分类ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteAllRelationsByCategory(Long categoryId) {
        // 1. 参数校验
        if (ObjectUtil.isNull(categoryId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }

        // 2. 删除关系
        int result = categoryRelationMapper.deleteByCategory(categoryId);
        // 即使没有删除任何记录，也认为操作成功
        return result >= 0;
    }

    /**
     * 批量删除指定分类的所有关系
     *
     * @param categoryIds 分类ID列表
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDeleteAllRelationsByCategories(List<Long> categoryIds) {
        // 1. 参数校验
        if (CollUtil.isEmpty(categoryIds)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID列表不能为空");
        }

        // 2. 批量删除关系
        int result = categoryRelationMapper.batchDeleteByCategories(categoryIds);

        // 即使没有删除任何记录，也认为操作成功
        return result >= 0;
    }

    /**
     * 根据内容获取关联的分类ID列表
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 分类ID列表
     */
    @Override
    public List<Long> getCategoryIdsByContent(String contentType, Long contentId) {
        // 1. 参数校验
        if (ObjectUtil.isNull(contentType) || ObjectUtil.isNull(contentId)) {
            return new ArrayList<>();
        }

        // 2. 查询分类ID列表
        return categoryRelationMapper.selectCategoryIdsByContent(contentType, contentId);
    }

    /**
     * 查询分类下有多少内容
     *
     * @param categoryId 分类ID
     * @param contentType 内容类型
     * @return 内容数量
     */
    @Override
    public Integer countContentsByCategory(Long categoryId, String contentType) {
        // 1. 参数校验
        if (ObjectUtil.isNull(categoryId)) {
            return 0;
        }

        // 2. 查询内容数量
        return categoryRelationMapper.countContentsByCategory(categoryId, contentType);
    }

    /**
     * 更新内容的分类关系
     *
     * @param categoryIds 新的分类ID列表
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateContentCategories(List<Long> categoryIds, String contentType, Long contentId) {
        // 1. 参数校验
        if (ObjectUtil.isNull(contentType) || ObjectUtil.isNull(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 先删除现有关系
        deleteAllRelationsByContent(contentType, contentId);

        // 3. 如果有新分类，创建新关系
        if (CollUtil.isNotEmpty(categoryIds)) {
            batchCreateCategoryRelations(categoryIds, contentType, contentId);
        }

        return true;
    }
}