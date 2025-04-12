package org.leocoder.picture.mapper;

import org.apache.ibatis.annotations.Param;
import org.leocoder.picture.domain.pojo.PictureHash;

import java.util.List;
import java.util.Set;

/**
 * @author : 程序员Leo
 * @date  2025-04-13 00:23
 * @version 1.0
 * @description :
 */

public interface PictureHashMapper {
    int deleteById(Long id);

    int insert(PictureHash record);

    int insertSelective(PictureHash record);

    PictureHash selectById(Long id);

    int updateByPrimaryKeySelective(PictureHash record);

    int updateById(PictureHash record);


    /**
     * 获取指定搜索词和抓取源下已存在的URL哈希集合
     */
    Set<String> getExistingUrlHashes(@Param("searchText") String searchText, @Param("source") String source);

    /**
     * 批量插入哈希记录
     */
    int batchInsert(@Param("list") List<PictureHash> pictureHashList);
}