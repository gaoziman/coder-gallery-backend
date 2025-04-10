package org.leocoder.picture.service;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 12:40
 * @description : 标签关系服务接口
 */
public interface TagRelationService {

    /**
     * 创建标签关系
     *
     * @param tagId 标签ID
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    Boolean createTagRelation(Long tagId, String contentType, Long contentId);

    /**
     * 批量创建标签关系
     *
     * @param tagIds 标签ID列表
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    Boolean batchCreateTagRelations(List<Long> tagIds, String contentType, Long contentId);

    /**
     * 删除标签关系
     *
     * @param tagId 标签ID
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    Boolean deleteTagRelation(Long tagId, String contentType, Long contentId);

    /**
     * 删除指定内容的所有标签关系
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    Boolean deleteAllRelationsByContent(String contentType, Long contentId);

    /**
     * 删除指定标签的所有关系
     *
     * @param tagId 标签ID
     * @return 是否成功
     */
    Boolean deleteAllRelationsByTag(Long tagId);

    /**
     * 批量删除标签的所有关系
     *
     * @param tagIds 标签ID列表
     * @return 是否成功
     */
    Boolean batchDeleteAllRelationsByTags(List<Long> tagIds);

    /**
     * 更新内容的标签关系
     *
     * @param tagIds 标签ID列表
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否成功
     */
    Boolean updateContentTags(List<Long> tagIds, String contentType, Long contentId);

    /**
     * 获取内容关联的标签ID列表
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 标签ID列表
     */
    List<Long> getTagIdsByContent(String contentType, Long contentId);

    /**
     * 查询标签下有多少内容
     *
     * @param tagId 标签ID
     * @param contentType 内容类型（可选）
     * @return 内容数量
     */
    Integer countContentsByTag(Long tagId, String contentType);

    /**
     * 检查标签关系是否存在
     *
     * @param tagId 标签ID
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 是否存在
     */
    Boolean checkRelationExists(Long tagId, String contentType, Long contentId);
}