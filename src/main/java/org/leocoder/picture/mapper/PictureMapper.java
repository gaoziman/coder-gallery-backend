package org.leocoder.picture.mapper;

import org.apache.ibatis.annotations.Param;
import org.leocoder.picture.domain.dto.picture.AdminPictureQueryRequest;
import org.leocoder.picture.domain.pojo.Picture;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 00:15
 * @description :
 */

public interface PictureMapper {
    int deleteById(Long id);

    int insert(Picture record);

    /**
     * 通过指定ID插入图片
     *
     * @param record 图片对象
     * @return 影响的行数
     */
    int insertWithId(Picture record);

    int insertSelective(Picture record);

    Picture selectById(Long id);

    int updateByPrimaryKeySelective(Picture record);


    /**
     * 根据ID更新图片记录
     *
     * @param record 图片记录
     * @return 影响的行数
     */
    int updateById(Picture record);

    /**
     * 查询上一张图片（ID小于当前图片且已审核通过的图片，按ID降序取第一个）
     *
     * @param currentId    当前图片ID
     * @param reviewStatus 审核状态（通常为已通过）
     * @return 上一张图片，如果没有则返回null
     */
    Picture selectPreviousPicture(@Param("currentId") Long currentId, @Param("reviewStatus") String reviewStatus);

    /**
     * 查询下一张图片（ID大于当前图片且已审核通过的图片，按ID升序取第一个）
     *
     * @param currentId    当前图片ID
     * @param reviewStatus 审核状态（通常为已通过）
     * @return 下一张图片，如果没有则返回null
     */
    Picture selectNextPicture(@Param("currentId") Long currentId, @Param("reviewStatus") String reviewStatus);

    /**
     * 增加图片浏览量
     *
     * @param id 图片ID
     * @return 影响的行数
     */
    int incrementViewCount(@Param("id") Long id);


    /**
     * 查询瀑布流图片列表（初始加载）
     *
     * @param reviewStatus 审核状态
     * @param format       图片格式
     * @param minWidth     最小宽度
     * @param minHeight    最小高度
     * @param userId       上传者ID
     * @param categoryId   分类ID
     * @param tagIds       标签ID列表
     * @param keyword      搜索关键词
     * @param sortBy       排序方式
     * @param limit        查询数量
     * @return 图片列表
     */
    List<Picture> selectWaterfallPictures(
            @Param("reviewStatus") String reviewStatus,
            @Param("format") String format,
            @Param("minWidth") Integer minWidth,
            @Param("minHeight") Integer minHeight,
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("tagIds") List<Long> tagIds,
            @Param("keyword") String keyword,
            @Param("sortBy") String sortBy,
            @Param("limit") Integer limit
    );


    /**
     * 加载更多瀑布流图片（基于最后一个ID和排序值）
     *
     * @param lastId       最后一张图片ID
     * @param lastValue    最后一个排序值（如时间戳、浏览量等）
     * @param reviewStatus 审核状态
     * @param format       图片格式
     * @param minWidth     最小宽度
     * @param minHeight    最小高度
     * @param userId       上传者ID
     * @param categoryId   分类ID
     * @param tagIds       标签ID列表
     * @param keyword      搜索关键词
     * @param sortBy       排序方式
     * @param limit        查询数量
     * @return 图片列表
     */
    List<Picture> selectMoreWaterfallPictures(
            @Param("lastId") Long lastId,
            @Param("lastValue") Object lastValue,
            @Param("reviewStatus") String reviewStatus,
            @Param("format") String format,
            @Param("minWidth") Integer minWidth,
            @Param("minHeight") Integer minHeight,
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("tagIds") List<Long> tagIds,
            @Param("keyword") String keyword,
            @Param("sortBy") String sortBy,
            @Param("limit") Integer limit
    );

    /**
     * 计算符合条件的图片总数
     *
     * @param reviewStatus 审核状态
     * @param format       图片格式
     * @param minWidth     最小宽度
     * @param minHeight    最小高度
     * @param userId       上传者ID
     * @param categoryId   分类ID
     * @param tagIds       标签ID列表
     * @param keyword      搜索关键词
     * @return 总数
     */
    Long countWaterfallPictures(
            @Param("reviewStatus") String reviewStatus,
            @Param("format") String format,
            @Param("minWidth") Integer minWidth,
            @Param("minHeight") Integer minHeight,
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("tagIds") List<Long> tagIds,
            @Param("keyword") String keyword
    );


    /**
     * 增加图片点赞数
     *
     * @param pictureId 图片ID
     * @return 更新成功的记录数
     */
    int incrementLikeCount(Long pictureId);

    /**
     * 减少图片点赞数
     *
     * @param pictureId 图片ID
     * @return 更新成功的记录数
     */
    int decrementLikeCount(Long pictureId);

    /**
     * 增加图片收藏数
     *
     * @param pictureId 图片ID
     * @return 更新成功的记录数
     */
    int incrementCollectionCount(Long pictureId);

    /**
     * 减少图片收藏数
     *
     * @param pictureId 图片ID
     * @return 更新成功的记录数
     */
    int decrementCollectionCount(Long pictureId);


    /**
     * 查询批量图片
     *
     * @param lastId 最后一个图片ID
     * @param limit  查询数量
     * @return 图片列表
     */
    List<Picture> selectBatchPictures(@Param("lastId") Long lastId, Integer limit);


    /**
     * 更新图片计数器
     * @param pictureId 图片ID
     * @param likeCount 点赞数
     * @param favoriteCount 收藏数
     */
    void updateCounters(@Param("pictureId") Long pictureId, @Param("likeCount") Long likeCount,@Param("favoriteCount") Long favoriteCount);


    /**
     * 查询相似图片列表
     *
     * @param pictureId       原图片ID
     * @param categoryId      原图片分类
     * @param tagIds          原图片标签列表
     * @param picScale        原图片宽高比
     * @param mainColor       原图片主色调
     * @param excludeUserId   排除的用户ID(当不包含同用户图片时)
     * @param matchType       匹配类型
     * @param relatedCategoryIds 相关分类ID列表
     * @return 相似图片列表
     */
    List<Picture> selectSimilarPictures(
            @Param("pictureId") Long pictureId,
            @Param("categoryId") Long categoryId,
            @Param("tagIds") List<Long> tagIds,
            @Param("picScale") Double picScale,
            @Param("mainColor") String mainColor,
            @Param("excludeUserId") Long excludeUserId,
            @Param("matchType") Integer matchType,
            @Param("relatedCategoryIds") List<Long> relatedCategoryIds
    );

    /**
     * 管理员查询图片列表
     *
     * @param queryRequest 查询参数
     * @return 图片列表
     */
    List<Picture> adminListPictures(AdminPictureQueryRequest queryRequest);

    /**
     * 批量逻辑删除图片
     *
     * @param pictureIds 图片ID列表
     * @param userId 操作用户ID
     * @return 影响的行数
     */
    int batchLogicDelete(@Param("pictureIds") List<Long> pictureIds, @Param("userId") Long userId);

    /**
     * 批量审核图片
     *
     * @param pictureIds 图片ID列表
     * @param reviewStatus 审核状态
     * @param reviewMessage 审核信息
     * @param reviewUserId 审核用户ID
     * @param reviewTime 审核时间
     * @return 影响的行数
     */
    int batchReview(
            @Param("pictureIds") List<Long> pictureIds,
            @Param("reviewStatus") String reviewStatus,
            @Param("reviewMessage") String reviewMessage,
            @Param("reviewUserId") Long reviewUserId,
            @Param("reviewTime") LocalDateTime reviewTime
    );


    /**
     * 获取今日上传图片数量
     *
     * @return 今日上传图片数量
     */
    Long countTodayUploads();

    /**
     * 获取本周上传图片数量
     *
     * @return 本周上传图片数量
     */
    Long countWeekUploads();

    /**
     * 获取本月上传图片数量
     *
     * @return 本月上传图片数量
     */
    Long countMonthUploads();

    /**
     * 获取指定审核状态的图片数量
     *
     * @param reviewStatus 审核状态
     * @return 指定状态的图片数量
     */
    Long countByReviewStatus(@Param("reviewStatus") String reviewStatus);

    /**
     * 获取图片总数
     *
     * @return 图片总数
     */
    Long countTotalPictures();

    /**
     * 获取上周上传图片数量
     *
     * @return 上周上传图片数量
     */
    Long countLastWeekUploads();

    /**
     * 获取上月上传图片数量
     *
     * @return 上月上传图片数量
     */
    Long countLastMonthUploads();

    /**
     * 获取当前总浏览量
     *
     * @return 当前总浏览量
     */
    Long getTotalViewCount();

    /**
     * 获取上周期总浏览量
     *
     * @return 上周期总浏览量
     */
    Long getLastPeriodViewCount();

    /**
     * 获取当前总点赞量
     *
     * @return 当前总点赞量
     */
    Long getTotalLikeCount();

    /**
     * 获取上周期总点赞量
     *
     * @return 上周期总点赞量
     */
    Long getLastPeriodLikeCount();
}