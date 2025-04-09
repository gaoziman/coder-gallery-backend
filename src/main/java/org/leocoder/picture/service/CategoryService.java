package org.leocoder.picture.service;

import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.domain.dto.category.*;
import org.leocoder.picture.domain.vo.category.CategoryStatisticsVO;
import org.leocoder.picture.domain.vo.category.CategoryTreeVO;
import org.leocoder.picture.domain.vo.category.CategoryVO;
import org.leocoder.picture.domain.vo.category.RelatedItemVO;

import java.util.List;

/**
 * @author : 程序员Leo
 * @date  2025-04-08 13:59
 * @version 1.0
 * @description : 分类服务接口
 */
public interface CategoryService {

    /**
     * 创建分类
     *
     * @param categoryCreateRequest 分类创建请求
     * @return 新创建的分类ID
     */
    Long createCategory(CategoryCreateRequest categoryCreateRequest);

    /**
     * 更新分类
     *
     * @param categoryUpdateRequest 分类更新请求
     * @return 是否成功
     */
    Boolean updateCategory(CategoryUpdateRequest categoryUpdateRequest);

    /**
     * 根据ID删除分类
     *
     * @param id 分类ID
     * @return 是否成功
     */
    Boolean deleteCategory(Long id);

    /**
     * 批量删除分类
     *
     * @param ids 分类ID列表
     * @return 是否成功
     */
    Boolean batchDeleteCategories(List<Long> ids);

    /**
     * 根据ID获取分类
     *
     * @param id 分类ID
     * @return 分类VO
     */
    CategoryVO getCategoryById(Long id);

    /**
     * 分页查询分类列表
     *
     * @param categoryQueryRequest 分类查询请求
     * @return 分页结果
     */
    PageResult<CategoryVO> listCategoryByPage(CategoryQueryRequest categoryQueryRequest);

    /**
     * 获取分类树形结构
     *
     * @param type 分类类型
     * @return 分类树形结构列表
     */
    List<CategoryTreeVO> getCategoryTree(String type);

    /**
     * 移动分类
     *
     * @param categoryMoveRequest 分类移动请求
     * @return 是否成功
     */
    Boolean moveCategory(CategoryMoveRequest categoryMoveRequest);

    /**
     * 批量移动分类
     *
     * @param batchCategoryMoveRequest 批量分类移动请求
     * @return 是否成功
     */
    Boolean batchMoveCategories(BatchCategoryMoveRequest batchCategoryMoveRequest);

    /**
     * 获取分类统计信息
     *
     * @return 分类统计信息
     */
    CategoryStatisticsVO getCategoryStatistics();

    /**
     * 获取分类关联的内容列表
     *
     * @param categoryId 分类ID
     * @param contentType 内容类型
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<RelatedItemVO> getCategoryRelatedItems(Long categoryId, String contentType, Integer pageNum, Integer pageSize);

    /**
     * 导出分类数据
     *
     * @param categoryExportRequest 分类导出请求
     * @return 导出文件URL
     */
    String exportCategories(CategoryExportRequest categoryExportRequest);

    /**
     * 获取分类列表(不分页)
     *
     * @param type 分类类型
     * @return 分类VO列表
     */
    List<CategoryVO> listCategoriesByType(String type);

    /**
     * 检查分类名称是否存在
     *
     * @param name 分类名称
     * @param parentId 父分类ID
     * @param type 分类类型
     * @param excludeId 排除的分类ID(更新时使用)
     * @return 是否存在
     */
    Boolean checkCategoryNameExists(String name, Long parentId, String type, Long excludeId);

    /**
     * 更新分类内容数量
     *
     * @param categoryId 分类ID
     * @param increment 增量(可为负)
     * @return 是否成功
     */
    Boolean updateCategoryContentCount(Long categoryId, Integer increment);

    /**
     * 递归获取所有子分类ID
     *
     * @param categoryId 分类ID
     * @return 子分类ID列表(包含自身)
     */
    List<Long> getAllChildCategoryIds(Long categoryId);
}