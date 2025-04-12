package org.leocoder.picture.service;

import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.domain.dto.tag.*;
import org.leocoder.picture.domain.vo.tag.*;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 12:35
 * @description : 标签服务接口
 */
public interface TagService {

    /**
     * 创建标签
     *
     * @param tagCreateRequest 标签创建请求
     * @return 新创建的标签ID
     */
    Long createTag(TagCreateRequest tagCreateRequest);

    /**
     * 更新标签
     *
     * @param tagUpdateRequest 标签更新请求
     * @return 是否成功
     */
    Boolean updateTag(TagUpdateRequest tagUpdateRequest);

    /**
     * 删除标签
     *
     * @param id 标签ID
     * @return 是否成功
     */
    Boolean deleteTag(Long id);

    /**
     * 批量删除标签
     *
     * @param ids 标签ID列表
     * @return 是否成功
     */
    Boolean batchDeleteTags(List<Long> ids);

    /**
     * 根据ID获取标签
     *
     * @param id 标签ID
     * @return 标签VO
     */
    TagVO getTagById(Long id);

    /**
     * 分页查询标签列表
     *
     * @param tagQueryRequest 标签查询请求
     * @return 分页结果
     */
    PageResult<TagVO> listTagByPage(TagQueryRequest tagQueryRequest);

    /**
     * 更新标签状态
     *
     * @param tagStatusUpdateRequest 标签状态更新请求
     * @return 是否成功
     */
    Boolean updateTagStatus(TagStatusUpdateRequest tagStatusUpdateRequest);

    /**
     * 批量更新标签状态
     *
     * @param batchTagStatusUpdateRequest 批量标签状态更新请求
     * @return 是否成功
     */
    Boolean batchUpdateTagStatus(TagBatchTagStatusUpdateRequest batchTagStatusUpdateRequest);

    /**
     * 获取标签统计信息
     *
     * @return 标签统计信息
     */
    TagStatisticsVO getTagStatistics();

    /**
     * 获取标签关联的内容列表
     *
     * @param tagId       标签ID
     * @param contentType 内容类型
     * @param pageNum     页码
     * @param pageSize    每页大小
     * @return 分页结果
     */
    PageResult<TagRelatedItemVO> getTagRelatedItems(Long tagId, String contentType, Integer pageNum, Integer pageSize);

    /**
     * 获取标签使用趋势数据
     *
     * @param tagId     标签ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 趋势数据列表
     */
    List<TagUsageTrendVO> getTagUsageTrend(Long tagId, String startDate, String endDate);


    /**
     * 检查标签名称是否存在
     *
     * @param name      标签名称
     * @param excludeId 排除的标签ID（更新时使用）
     * @return 是否存在
     */
    Boolean checkTagNameExists(String name, Long excludeId);

    /**
     * 更新标签引用次数
     *
     * @param tagId     标签ID
     * @param increment 增量（可为负）
     * @return 是否成功
     */
    Boolean updateTagReferenceCount(Long tagId, Integer increment);


    /**
     * 获取标签列表
     *
     * @return 标签列表
     */
    List<TagVO> getTagList();


}