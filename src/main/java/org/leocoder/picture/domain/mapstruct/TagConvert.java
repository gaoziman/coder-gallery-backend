package org.leocoder.picture.domain.mapstruct;

import org.leocoder.picture.domain.pojo.Tag;
import org.leocoder.picture.domain.vo.tag.TagVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

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
     * @param tag 标签实体
     * @return 标签VO
     */
    TagVO toTagVO(Tag tag);

    /**
     * 将 Tag 实体列表转换为 TagVO 列表
     * @param tagList 标签实体列表
     * @return 标签VO列表
     */
    List<TagVO> toTagVOList(List<Tag> tagList);
}