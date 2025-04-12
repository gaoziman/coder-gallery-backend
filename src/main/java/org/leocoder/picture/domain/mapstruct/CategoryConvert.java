package org.leocoder.picture.domain.mapstruct;

import org.leocoder.picture.domain.pojo.Category;
import org.leocoder.picture.domain.vo.category.CategoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-13
 * @description : 分类对象转换接口
 */
@Mapper
public interface CategoryConvert {
    CategoryConvert INSTANCE = Mappers.getMapper(CategoryConvert.class);

    /**
     * 将 Category 实体转换为 CategoryVO
     * 注意：parentName, createUsername, updateUsername 需要在服务层单独设置
     * @param category 分类实体
     * @return 分类VO
     */
    CategoryVO toCategoryVO(Category category);

    /**
     * 将 Category 实体列表转换为 CategoryVO 列表
     * @param categoryList 分类实体列表
     * @return 分类VO列表
     */
    List<CategoryVO> toCategoryVOList(List<Category> categoryList);
}