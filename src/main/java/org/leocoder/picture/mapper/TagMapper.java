// 在TagMapper.java中添加或修改以下方法声明
package org.leocoder.picture.mapper;

import org.apache.ibatis.annotations.Param;
import org.leocoder.picture.domain.pojo.Tag;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 11:20
 * @description : 标签Mapper接口 - 扩展方法
 */
public interface TagMapper {

    /**
     * 根据ID查询标签
     *
     * @param id 标签ID
     * @return 标签对象
     */
    Tag selectById(Long id);

    /**
     * 插入标签
     *
     * @param tag 标签对象
     * @return 影响的行数
     */
    int insert(Tag tag);

    /**
     * 使用指定Id插入标签
     *
     * @param tag 标签对象
     * @return 影响的行数
     */
    int insertTagWithId(Tag tag);


    /**
     * 选择性更新标签
     *
     * @param tag 标签对象
     * @return 影响的行数
     */
    int updateByPrimaryKeySelective(Tag tag);

    /**
     * 根据名称查询标签
     *
     * @param name 标签名称
     * @return 标签对象
     */
    Tag selectByName(String name);

    /**
     * 根据条件分页查询标签
     *
     * @param name 标签名称（模糊查询）
     * @param status 标签状态
     * @param category 标签分类
     * @return 标签列表
     */

    /**
     * 根据条件查询标签列表
     *
     * @param name            标签名称
     * @param status          标签状态
     * @param createTimeStart 创建开始时间
     * @param createTimeEnd   创建结束时间
     * @return 标签列表
     */
    List<Tag> selectByCondition(@Param("name") String name, @Param("status") String status,
                                @Param("createTimeStart")String createTimeStart, @Param("createTimeEnd")String createTimeEnd);

    /**
     * 更新标签引用次数
     *
     * @param id        标签ID
     * @param increment 增量值
     * @return 影响的行数
     */
    int updateReferenceCount(@Param("id") Long id, @Param("increment") Integer increment);

    /**
     * 批量更新标签状态
     *
     * @param ids    标签ID列表
     * @param status 状态
     * @return 影响的行数
     */
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 逻辑删除标签
     *
     * @param id 标签ID
     * @return 影响的行数
     */
    int logicalDelete(Long id);

    /**
     * 批量逻辑删除标签
     *
     * @param ids 标签ID列表
     * @return 影响的行数
     */
    int batchLogicalDelete(@Param("ids") List<Long> ids);

    /**
     * 统计标签总数
     *
     * @param status 状态（可选）
     * @return 标签数量
     */
    int countTags(@Param("status") String status);


    /**
     * 统计今日创建的标签数量
     *
     * @return 标签数量
     */
    Integer countTodayTags();

    /**
     * 统计本周新增的标签数量
     *
     * @return 标签数量
     */
    Integer countWeekTags();

    /**
     * 统计本月新增的标签数量
     *
     * @return 标签数量
     */
    Integer countMonthTags();

    /**
     * 统计未使用的标签数量（引用计数为0）
     *
     * @return 标签数量
     */
    Integer countUnusedTags();

    /**
     * 计算所有标签的引用总数
     *
     * @return 引用总数
     */
    Integer sumTagReferenceCount();
}