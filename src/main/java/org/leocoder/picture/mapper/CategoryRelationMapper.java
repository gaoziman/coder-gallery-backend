package org.leocoder.picture.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.leocoder.picture.domain.pojo.CategoryRelation;
import org.leocoder.picture.domain.vo.category.RelatedItemVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 分类关系Mapper
 */
@Mapper
public interface CategoryRelationMapper {

    /**
     * 根据ID查询分类关系
     *
     * @param id 关系ID
     * @return 分类关系
     */
    CategoryRelation selectById(Long id);

    /**
     * 根据ID删除分类关系
     *
     * @param id 关系ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 插入分类关系
     *
     * @param categoryRelation 分类关系
     * @return 影响行数
     */
    int insert(CategoryRelation categoryRelation);

    /**
     * 选择性插入分类关系
     *
     * @param categoryRelation 分类关系
     * @return 影响行数
     */
    int insertSelective(CategoryRelation categoryRelation);

    /**
     * 选择性更新分类关系
     *
     * @param categoryRelation 分类关系
     * @return 影响行数
     */
    int updateByPrimaryKeySelective(CategoryRelation categoryRelation);

    /**
     * 更新分类关系
     *
     * @param categoryRelation 分类关系
     * @return 影响行数
     */
    int updateById(CategoryRelation categoryRelation);

    /**
     * 批量插入分类关系
     *
     * @param relationList 分类关系列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<CategoryRelation> relationList);

    /**
     * 查询分类关系
     *
     * @param categoryId 分类ID
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 分类关系
     */
    CategoryRelation selectByCondition(@Param("categoryId") Long categoryId,
                                       @Param("contentType") String contentType,
                                       @Param("contentId") Long contentId);

    /**
     * 根据内容删除所有分类关系
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 影响行数
     */
    int deleteByContent(@Param("contentType") String contentType,
                        @Param("contentId") Long contentId);

    /**
     * 根据分类ID删除分类关系
     *
     * @param categoryId 分类ID
     * @return 影响行数
     */
    int deleteByCategory(@Param("categoryId") Long categoryId);

    /**
     * 批量删除指定分类的所有关系
     *
     * @param categoryIds 分类ID列表
     * @return 影响行数
     */
    int batchDeleteByCategories(@Param("list") List<Long> categoryIds);

    /**
     * 根据内容查询关联的分类ID列表
     *
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @return 分类ID列表
     */
    List<Long> selectCategoryIdsByContent(@Param("contentType") String contentType,
                                          @Param("contentId") Long contentId);

    /**
     * 统计分类下内容数量
     *
     * @param categoryId 分类ID
     * @param contentType 内容类型
     * @return 内容数量
     */
    int countContentsByCategory(@Param("categoryId") Long categoryId,
                                @Param("contentType") String contentType);

    /**
     * 分页查询分类关联的内容
     *
     * @param categoryId 分类ID
     * @param contentType 内容类型
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 关联内容列表
     */
    List<RelatedItemVO> selectCategoryRelatedItems(@Param("categoryId") Long categoryId,
                                                   @Param("contentType") String contentType,
                                                   @Param("offset") Integer offset,
                                                   @Param("pageSize") Integer pageSize);

    /**
     * 统计分类关联的内容数量
     *
     * @param categoryId 分类ID
     * @param contentType 内容类型
     * @return 内容数量
     */
    Long countCategoryRelatedItems(@Param("categoryId") Long categoryId,
                                   @Param("contentType") String contentType);

    /**
     * 逻辑删除分类关系
     *
     * @param id 关系ID
     * @param updateTime 更新时间
     * @param updateUser 更新用户
     * @return 影响行数
     */
    int logicDeleteRelation(@Param("id") Long id,
                            @Param("updateTime") LocalDateTime updateTime,
                            @Param("updateUser") Long updateUser);
}