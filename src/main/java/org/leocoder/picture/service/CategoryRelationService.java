package org.leocoder.picture.service;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 13:59
 * @description : 分类关系服务接口
 */
public interface CategoryRelationService {

    /**
     * 创建分类关系
     *
     * @param categoryId  分类ID
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 是否成功
     */
    Boolean createCategoryRelation(Long categoryId, String contentType, Long contentId);

    /**
     * 批量创建分类关系
     *
     * @param categoryIds 分类ID列表
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 是否成功
     */
    Boolean batchCreateCategoryRelations(List<Long> categoryIds, String contentType, Long contentId);

    /**
     * 删除分类关系
     *
     * @param categoryId  分类ID
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 是否成功
     */
    Boolean deleteCategoryRelation(Long categoryId, String contentType, Long contentId);

    /**
     * 删除指定内容的所有分类关系
     *
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 是否成功
     */
    Boolean deleteAllRelationsByContent(String contentType, Long contentId);

    /**
     * 删除指定分类的所有关系
     *
     * @param categoryId 分类ID
     * @return 是否成功
     */
    Boolean deleteAllRelationsByCategory(Long categoryId);

    /**
     * 批量删除指定分类的所有关系
     *
     * @param categoryIds 分类ID列表
     * @return 是否成功
     */
    Boolean batchDeleteAllRelationsByCategories(List<Long> categoryIds);

    /**
     * 根据内容获取关联的分类ID列表
     *
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 分类ID列表
     */
    List<Long> getCategoryIdsByContent(String contentType, Long contentId);

    /**
     * 查询分类下有多少内容
     *
     * @param categoryId  分类ID
     * @param contentType 内容类型
     * @return 内容数量
     */
    Integer countContentsByCategory(Long categoryId, String contentType);

    /**
     * 更新内容的分类关系
     *
     * @param categoryIds 新的分类ID列表
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 是否成功
     */
    Boolean updateContentCategories(List<Long> categoryIds, String contentType, Long contentId);
}