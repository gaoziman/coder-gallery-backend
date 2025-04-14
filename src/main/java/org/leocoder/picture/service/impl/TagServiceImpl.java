package org.leocoder.picture.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.common.PageUtils;
import org.leocoder.picture.domain.dto.tag.*;
import org.leocoder.picture.domain.mapstruct.TagConvert;
import org.leocoder.picture.domain.pojo.Tag;
import org.leocoder.picture.domain.vo.tag.TagRelatedItemVO;
import org.leocoder.picture.domain.vo.tag.TagStatisticsVO;
import org.leocoder.picture.domain.vo.tag.TagUsageTrendVO;
import org.leocoder.picture.domain.vo.tag.TagVO;
import org.leocoder.picture.enums.TagStatusEnum;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.mapper.TagMapper;
import org.leocoder.picture.mapper.TagRelationMapper;
import org.leocoder.picture.service.TagRelationService;
import org.leocoder.picture.service.TagService;
import org.leocoder.picture.utils.SnowflakeIdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 11:00
 * @description : 标签服务实现类
 */
@Slf4j
@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    private final TagRelationMapper tagRelationMapper;

    private TagRelationService tagRelationService;


    private final SnowflakeIdGenerator snowflakeIdGenerator;

    // 使用构造器注入除了 TagRelationService 以外的依赖
    public TagServiceImpl(TagMapper tagMapper,
                          TagRelationMapper tagRelationMapper,
                          SnowflakeIdGenerator idGenerator) {
        this.tagMapper = tagMapper;
        this.tagRelationMapper = tagRelationMapper;
        this.snowflakeIdGenerator = idGenerator;
    }


    /**
     * 创建标签
     *
     * @param requestParam 标签创建请求
     * @return 新创建的标签ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTag(TagCreateRequest requestParam) {
        // 校验标签名称是否已存在
        if (checkTagNameExists(requestParam.getName(), null)) {
            throw new BusinessException(ErrorCode.TAG_NAME_ALREADY_EXISTS);
        }

        // 生成必要的基础数据
        Long tagId = snowflakeIdGenerator.nextId();
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        // 使用MapStruct转换请求对象为实体
        Tag tag = TagConvert.INSTANCE.toTag(requestParam, tagId, userId, now);

        // 插入数据库
        tagMapper.insertTagWithId(tag);
        log.info("标签创建成功: {}", tag.getId());

        return tag.getId();
    }


    /**
     * 更新标签
     *
     * @param requestParam 标签更新请求
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTag(TagUpdateRequest requestParam) {
        // 校验标签是否存在
        Tag existingTag = tagMapper.selectById(requestParam.getId());
        if (existingTag == null || existingTag.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }

        // 校验标签名称是否已存在（排除自身）
        if (checkTagNameExists(requestParam.getName(), requestParam.getId())) {
            throw new BusinessException(ErrorCode.TAG_NAME_ALREADY_EXISTS);
        }

        // 更新标签信息
        Tag tag = Tag.builder()
                .id(requestParam.getId())
                .name(requestParam.getName())
                .color(requestParam.getColor())
                .description(requestParam.getDescription())
                .sortOrder(requestParam.getSortOrder())
                .updateTime(LocalDateTime.now())
                .updateUser(StpUtil.getLoginIdAsLong())
                .build();

        // 更新数据库
        tagMapper.updateByPrimaryKeySelective(tag);
        log.info("标签更新成功: {}", tag.getId());

        return true;
    }


    /**
     * 删除标签
     *
     * @param id 标签ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTag(Long id) {
        // 校验标签是否存在
        Tag existingTag = tagMapper.selectById(id);
        if (existingTag == null || existingTag.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }

        // 检查标签是否有关联内容
        int relatedCount = tagRelationMapper.countByTagId(id, null);
        if (relatedCount > 0) {
            // 如果有关联内容，先删除关联关系
            tagRelationService.deleteAllRelationsByTag(id);
        }

        // 逻辑删除标签
        tagMapper.logicalDelete(id);
        log.info("标签删除成功: {}", id);

        return true;
    }


    /**
     * 批量删除标签
     *
     * @param ids 标签ID列表
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDeleteTags(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return false;
        }

        // 逻辑删除标签关系
        tagRelationMapper.batchDeleteByTagIds(ids);

        // 逻辑删除标签
        tagMapper.batchLogicalDelete(ids);
        log.info("批量删除标签成功, 数量: {}", ids.size());

        return true;
    }


    /**
     * 根据ID获取标签
     *
     * @param id 标签ID
     * @return 标签VO
     */
    @Override
    public TagVO getTagById(Long id) {
        // 查询标签
        Tag tag = tagMapper.selectById(id);
        if (tag == null || tag.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }

        // 使用MapStruct转换为VO
        return TagConvert.INSTANCE.toTagVO(tag);
    }

    /**
     * 分页查询标签列表
     *
     * @param requestParam 标签查询请求
     * @return 分页结果
     */
    @Override
    public PageResult<TagVO> listTagByPage(TagQueryRequest requestParam) {
        // 使用 PageUtils 和 MapStruct 进行分页查询和对象转换
        return PageUtils.doPage(
                requestParam,
                () -> tagMapper.selectByCondition(
                        requestParam.getName(),
                        requestParam.getStatus(),
                        requestParam.getCreateTimeStart(),
                        requestParam.getCreateTimeEnd()
                ),
                tag -> {
                    // 使用 MapStruct 转换基本属性
                    TagVO tagVO = TagConvert.INSTANCE.toTagVO(tag);

                    // // 设置创建人信息
                    // if (tag.getCreateUser() != null) {
                    //     try {
                    //         User user = userService.getUsernameById(tag.getCreateUser());
                    //         if (user != null) {
                    //             tagVO.setCreateUser(user.getId());
                    //         }
                    //     } catch (Exception e) {
                    //         log.warn("获取用户名失败: {}", tag.getCreateUser(), e);
                    //     }
                    // }

                    return tagVO;
                }
        );
    }


    /**
     * 更新标签状态
     *
     * @param requestParam 标签状态更新请求
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTagStatus(TagStatusUpdateRequest requestParam) {
        // 校验标签是否存在
        Tag existingTag = tagMapper.selectById(requestParam.getId());
        if (existingTag == null || existingTag.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }

        // 更新状态
        Tag tag = Tag.builder()
                .id(requestParam.getId())
                .status(requestParam.getStatus())
                .updateTime(LocalDateTime.now())
                .updateUser(StpUtil.getLoginIdAsLong())
                .build();

        tagMapper.updateByPrimaryKeySelective(tag);
        log.info("标签状态更新成功: {}, 新状态: {}", tag.getId(), requestParam.getStatus());

        return true;
    }

    /**
     * 批量更新标签状态
     *
     * @param requestParam 批量标签状态更新请求
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchUpdateTagStatus(TagBatchTagStatusUpdateRequest requestParam) {
        if (CollUtil.isEmpty(requestParam.getIds())) {
            return false;
        }

        // 批量更新状态
        tagMapper.batchUpdateStatus(requestParam.getIds(), requestParam.getStatus());
        log.info("批量更新标签状态成功, 数量: {}, 新状态: {}",
                requestParam.getIds().size(), requestParam.getStatus());

        return true;
    }


    /**
     * 获取标签统计信息
     *
     * @return 标签统计信息
     */
    @Override
    public TagStatisticsVO getTagStatistics() {
        TagStatisticsVO vo = new TagStatisticsVO();

        // 获取总标签数
        vo.setTotalCount(tagMapper.countTags(null));

        // 获取启用中的标签数
        vo.setActiveCount(tagMapper.countTags(TagStatusEnum.ACTIVE.getValue()));

        // 获取未启用的标签数
        vo.setInactiveCount(tagMapper.countTags(TagStatusEnum.DISABLED.getValue()));

        // 获取今日创建的标签数
        vo.setTodayCount(tagMapper.countTodayTags());

        // 获取本周新增的标签数
        vo.setWeekCount(tagMapper.countWeekTags());

        // 获取本月新增的标签数
        vo.setMonthCount(tagMapper.countMonthTags());

        // 获取未使用的标签数（引用计数为0）
        vo.setUnusedTag(tagMapper.countUnusedTags());

        // 获取所有标签的引用总数
        vo.setTotalReferenceCount(tagMapper.sumTagReferenceCount());

        return vo;
    }

    /**
     * 获取标签关联的内容列表
     *
     * @param tagId       标签ID
     * @param contentType 内容类型
     * @param pageNum     页码
     * @param pageSize    每页大小
     * @return 分页结果
     */
    @Override
    public PageResult<TagRelatedItemVO> getTagRelatedItems(Long tagId, String contentType,
                                                           Integer pageNum, Integer pageSize) {
        // 校验标签是否存在
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null || tag.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }

        // 获取关联内容ID列表
        List<Long> contentIds = tagRelationMapper.selectContentIdsByTag(tagId, contentType);
        long total = contentIds.size();

        // 手动分页
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, contentIds.size());
        List<Long> pageContentIds = (startIndex < contentIds.size())
                ? contentIds.subList(startIndex, endIndex)
                : new ArrayList<>();

        // 根据内容ID查询详细信息（需要根据contentType调用不同的服务）
        // 这里简化处理，实际应用中需要根据contentType调用不同的服务获取详细信息
        List<TagRelatedItemVO> relatedItems = new ArrayList<>();
        for (Long contentId : pageContentIds) {
            TagRelatedItemVO item = new TagRelatedItemVO();
            item.setId(contentId);
            item.setType(contentType);
            // 设置其他属性...

            relatedItems.add(item);
        }

        // 构建分页结果
        return PageResult.build(total, relatedItems, pageNum, pageSize);
    }

    /**
     * 获取标签使用趋势数据
     *
     * @param tagId     标签ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 趋势数据列表
     */
    @Override
    public List<TagUsageTrendVO> getTagUsageTrend(Long tagId, String startDate, String endDate) {
        // 校验标签是否存在
        Tag tag = tagMapper.selectById(tagId);
        if (ObjectUtil.isNull(tag) || tag.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }

        // 查询使用趋势数据
        List<Map<String, Object>> trendData = tagRelationMapper.selectUsageTrend(
                tagId, startDate, endDate);

        // 转换为VO
        List<TagUsageTrendVO> result = new ArrayList<>();
        for (Map<String, Object> data : trendData) {
            TagUsageTrendVO vo = new TagUsageTrendVO();
            vo.setDate((String) data.get("date"));
            vo.setCount(((Number) data.get("count")).intValue());
            result.add(vo);
        }

        return result;
    }


    /**
     * 检查标签名称是否存在
     *
     * @param name      标签名称
     * @param excludeId 排除的标签ID（更新时使用）
     * @return 是否存在
     */
    @Override
    public Boolean checkTagNameExists(String name, Long excludeId) {
        Tag existingTag = tagMapper.selectByName(name);
        if (existingTag == null || existingTag.getIsDeleted() == 1) {
            return false;
        }

        // 如果是更新操作，需要排除自身
        if (ObjectUtil.isNotNull(excludeId) && existingTag.getId().equals(excludeId)) {
            return false;
        }

        return true;
    }


    /**
     * 更新标签引用次数
     *
     * @param tagId     标签ID
     * @param increment 增量（可为负）
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTagReferenceCount(Long tagId, Integer increment) {
        // 校验标签是否存在
        Tag tag = tagMapper.selectById(tagId);
        if (ObjectUtil.isNull(tag) || tag.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }

        // 更新引用次数
        tagMapper.updateReferenceCount(tagId, increment);
        log.info("更新标签引用次数成功: {}, 增量: {}", tagId, increment);

        return true;
    }


    /**
     * 获取标签列表
     *
     * @return 标签列表
     */
    @Override
    public List<TagVO> getTagList() {
        return tagMapper.selectTagList();
    }

}