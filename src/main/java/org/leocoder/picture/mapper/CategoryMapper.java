package org.leocoder.picture.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.leocoder.picture.domain.dto.category.CategoryQueryRequest;
import org.leocoder.picture.domain.pojo.Category;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 分类Mapper
 */
@Mapper
public interface CategoryMapper {

    /**
     * 根据ID查询分类
     *
     * @param id 分类ID
     * @return 分类
     */
    Category selectById(Long id);

    /**
     * 根据ID删除分类
     *
     * @param id 分类ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 插入分类
     *
     * @param category 分类
     * @return 影响行数
     */
    int insert(Category category);

    /**
     * 选择性插入分类
     *
     * @param category 分类
     * @return 影响行数
     */
    int insertSelective(Category category);

    /**
     * 选择性更新分类
     *
     * @param category 分类
     * @return 影响行数
     */
    int updateByPrimaryKeySelective(Category category);

    /**
     * 更新分类
     *
     * @param category 分类
     * @return 影响行数
     */
    int updateById(Category category);

    /**
     * 根据名称和父ID查询分类
     *
     * @param name     分类名称
     * @param parentId 父分类ID
     * @param type     分类类型
     * @return 分类
     */
    Category selectByNameAndParent(@Param("name") String name, @Param("parentId") Long parentId, @Param("type") String type);

    /**
     * 根据父ID查询子分类列表
     *
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    List<Category> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 根据条件查询分类数量
     *
     * @param queryRequest    查询条件
     * @param createTimeStart 创建开始时间
     * @param createTimeEnd   创建结束时间
     * @return 分类数量
     */
    Long countCategories(@Param("queryRequest") CategoryQueryRequest queryRequest,
                         @Param("createTimeStart") LocalDateTime createTimeStart,
                         @Param("createTimeEnd") LocalDateTime createTimeEnd);

    /**
     * 分页查询分类列表
     *
     * @param createTimeStart 创建开始时间
     * @param createTimeEnd   创建结束时间
     * @return 分类列表
     */
    List<Category> listCategoriesByPage(
            @Param("queryRequest") CategoryQueryRequest requestParam,
            @Param("createTimeStart") LocalDateTime createTimeStart,
            @Param("createTimeEnd") LocalDateTime createTimeEnd
    );

    /**
     * 根据类型查询分类列表
     *
     * @param type 分类类型
     * @return 分类列表
     */
    List<Category> selectByType(@Param("type") String type);

    /**
     * 逻辑删除分类
     *
     * @param id         分类ID
     * @param updateTime 更新时间
     * @param updateUser 更新用户
     * @return 影响行数
     */
    int logicDeleteCategory(@Param("id") Long id,
                            @Param("updateTime") LocalDateTime updateTime,
                            @Param("updateUser") Long updateUser);

    /**
     * 批量逻辑删除分类
     *
     * @param ids        分类ID列表
     * @param updateTime 更新时间
     * @param updateUser 更新用户
     * @return 影响行数
     */
    int batchLogicDeleteCategories(@Param("ids") List<Long> ids,
                                   @Param("updateTime") LocalDateTime updateTime,
                                   @Param("updateUser") Long updateUser);

    /**
     * 更新分类路径
     *
     * @param id         分类ID
     * @param path       路径
     * @param updateTime 更新时间
     * @param updateUser 更新用户
     * @return 影响行数
     */
    int updateCategoryPath(@Param("id") Long id,
                           @Param("path") String path,
                           @Param("updateTime") LocalDateTime updateTime,
                           @Param("updateUser") Long updateUser);

    /**
     * 统计顶级分类数量
     *
     * @param type 分类类型
     * @return 顶级分类数量
     */
    Long countTopLevelCategories(@Param("type") String type);

    /**
     * 获取最大分类层级
     *
     * @return 最大层级
     */
    Integer getMaxCategoryLevel();

    /**
     * 统计今日新增分类数
     *
     * @return 今日新增分类数
     */
    Long countNewCategoriesOfToday();

    /**
     * 统计本月新增分类数
     *
     * @return 本月新增分类数
     */
    Long countNewCategoriesOfMonth();

    /**
     * 统计上月分类总数
     *
     * @return 上月分类总数
     */
    Long countLastMonthTotalCategories();

    /**
     * 获取内容数量最多的分类
     *
     * @return 分类
     */
    Category getMostContentsCategory();

    /**
     * 统计激活状态的分类数量
     *
     * @return 激活状态的分类数量
     */
    Long countActiveCategories();

    /**
     * 统计禁用状态的分类数量
     *
     * @return 禁用状态的分类数量
     */
    Long countDisabledCategories();

    /**
     * 更新分类内容数量
     *
     * @param id         分类ID
     * @param increment  增量
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int updateContentCount(@Param("id") Long id,
                           @Param("increment") Integer increment,
                           @Param("updateTime") LocalDateTime updateTime);

    /**
     * 使用指定ID插入分类
     *
     * @param category 分类
     * @return 影响行数
     */
    int insertWithId(Category category);


    /**
     * 根据urlName查询分类数量
     *
     * @param params 查询参数
     * @return 分类数量
     */
    Integer countByUrlName(Map<String, Object> params);


    /**
     *  批量获取分类
     * @param longs 分类ID列表
     * @return 分类列表
     */
    List<Category> selectBatchIds(@Param("ids") ArrayList<Long> longs);
}