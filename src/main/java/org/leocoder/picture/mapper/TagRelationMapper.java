package org.leocoder.picture.mapper;

import org.apache.ibatis.annotations.Param;
import org.leocoder.picture.domain.pojo.TagRelation;
import java.util.List;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 11:25
 * @description : 标签关系Mapper接口 - 扩展方法
 */
public interface TagRelationMapper {

    /**
     * 根据ID查询标签关系
     *
     * @param id 标签关系ID
     * @return 标签关系对象
     */
    TagRelation selectById(Long id);

    /**
     * 插入标签关系
     *
     * @param tagRelation 标签关系对象
     * @return 影响的行数
     */
    int insert(TagRelation tagRelation);

    /**
     * 选择性更新标签关系
     *
     * @param tagRelation 标签关系对象
     * @return 影响的行数
     */
    int updateByPrimaryKeySelective(TagRelation tagRelation);

    /**
     * 根据ID删除标签关系
     *
     * @param id 标签关系ID
     * @return 影响的行数
     */
    int deleteById(Long id);

    /**
     * 根据标签ID和内容信息查询关系
     *
     * @param tagId 标签ID
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 标签关系
     */
    TagRelation selectByTagAndContent(@Param("tagId") Long tagId,
                                      @Param("contentType") String contentType,
                                      @Param("contentId") Long contentId);

    /**
     * 根据内容信息查询关联的标签ID列表
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 标签ID列表
     */
    List<Long> selectTagIdsByContent(@Param("contentType") String contentType,
                                     @Param("contentId") Long contentId);

    /**
     * 根据内容信息删除所有标签关系
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 影响的行数
     */
    int deleteByContent(@Param("contentType") String contentType,
                        @Param("contentId") Long contentId);

    /**
     * 根据标签ID删除所有关系
     *
     * @param tagId 标签ID
     * @return 影响的行数
     */
    int deleteByTagId(Long tagId);

    /**
     * 批量删除标签关系
     *
     * @param tagIds 标签ID列表
     * @return 影响的行数
     */
    int batchDeleteByTagIds(@Param("tagIds") List<Long> tagIds);

    /**
     * 统计标签关联的内容数量
     *
     * @param tagId 标签ID
     * @param contentType 内容类型（可选）
     * @return 关联数量
     */
    int countByTagId(@Param("tagId") Long tagId,
                     @Param("contentType") String contentType);

    /**
     * 获取标签关联的内容列表
     *
     * @param tagId 标签ID
     * @param contentType 内容类型
     * @return 内容ID列表
     */
    List<Long> selectContentIdsByTag(@Param("tagId") Long tagId,
                                     @Param("contentType") String contentType);

    /**
     * 查询标签使用趋势
     *
     * @param tagId 标签ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 日期和使用次数
     */
    List<Map<String, Object>> selectUsageTrend(@Param("tagId") Long tagId,
                                               @Param("startDate") String startDate,
                                               @Param("endDate") String endDate);
}