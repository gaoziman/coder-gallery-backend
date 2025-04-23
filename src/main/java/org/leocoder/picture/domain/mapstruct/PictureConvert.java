package org.leocoder.picture.domain.mapstruct;

import org.leocoder.picture.domain.pojo.Picture;
import org.leocoder.picture.domain.vo.picture.PictureVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-23 10:53
 * @description : 图片对象转换器
 */
@Mapper
public interface PictureConvert {
    PictureConvert INSTANCE = Mappers.getMapper(PictureConvert.class);

    /**
     * 图片实体转VO
     */
    PictureVO toPictureVO(Picture picture);

    /**
     * 图片实体列表转VO列表
     */
    List<PictureVO> toPictureVOList(List<Picture> pictureList);
}