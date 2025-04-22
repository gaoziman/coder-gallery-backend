package org.leocoder.picture.domain.mapstruct;

import org.leocoder.picture.domain.dto.tag.TagCreateRequest;
import org.leocoder.picture.domain.pojo.Tag;
import org.leocoder.picture.domain.vo.tag.TagVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-13
 * @description : 标签对象转换接口
 */
@Mapper
public interface TagConvert {
    TagConvert INSTANCE = Mappers.getMapper(TagConvert.class);

    /**
     * 将 Tag 实体转换为 TagVO
     *
     * @param tag 标签实体
     * @return 标签VO
     */
    TagVO toTagVO(Tag tag);

    /**
     * 将 Tag 实体列表转换为 TagVO 列表
     *
     * @param tagList 标签实体列表
     * @return 标签VO列表
     */
    List<TagVO> toTagVOList(List<Tag> tagList);


    /**
     * 将创建请求转换为标签实体
     *
     * @param request 创建请求
     * @param id      标签ID
     * @param userId  创建者ID
     * @param now     创建时间
     * @return 标签实体
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "createUser", source = "userId")
    @Mapping(target = "updateUser", source = "userId")
    @Mapping(target = "createTime", source = "now")
    @Mapping(target = "updateTime", source = "now")
    @Mapping(target = "status", constant = "active")
    @Mapping(target = "referenceCount", constant = "0")
    @Mapping(target = "isDeleted", constant = "0")
    Tag toTag(TagCreateRequest request, Long id, Long userId, LocalDateTime now);
}