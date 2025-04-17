package org.leocoder.picture.service;

import org.leocoder.picture.domain.userreaction.UserReactionRequest;
import org.leocoder.picture.domain.vo.picture.PictureVO;
import org.leocoder.picture.domain.vo.userreaction.UserReactionCountVO;
import org.leocoder.picture.domain.vo.userreaction.UserReactionStatusVO;

import java.util.List;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @date  2025-04-16 21:53
 * @version 1.0
 * @description :
 */

public interface UserReactionService {

    /**
     * 添加用户点赞/收藏（点赞、收藏等）
     * @param request 点赞/收藏请求
     * @param userId 用户ID
     * @return 操作是否成功
     */
    boolean addReaction(UserReactionRequest request, Long userId);

    /**
     * 取消用户点赞/收藏
     * @param request 点赞/收藏请求
     * @param userId 用户ID
     * @return 操作是否成功
     */
    boolean removeReaction(UserReactionRequest request, Long userId);

    /**
     * 查询用户对特定目标的点赞/收藏状态
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @param userId 用户ID
     * @return 点赞/收藏状态（包含是否点赞、收藏等）
     */
    UserReactionStatusVO getUserReactionStatus(String targetType, Long targetId, Long userId);

    /**
     * 批量查询用户对多个目标的点赞/收藏状态
     * @param targetType 目标类型
     * @param targetIds 目标ID列表
     * @param userId 用户ID
     * @return 目标ID到点赞/收藏状态的映射
     */
    Map<Long, UserReactionStatusVO> batchGetUserReactionStatus(String targetType, List<Long> targetIds, Long userId);

    /**
     * 获取目标的点赞/收藏计数
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @return 点赞/收藏计数（包含点赞数、收藏数等）
     */
    UserReactionCountVO getReactionCounts(String targetType, Long targetId);

    /**
     * 批量获取多个目标的点赞/收藏计数
     * @param targetType 目标类型
     * @param targetIds 目标ID列表
     * @return 目标ID到点赞/收藏计数的映射
     */
    Map<Long, UserReactionCountVO> batchGetReactionCounts(String targetType, List<Long> targetIds);

    /**
     * 获取热门图片列表
     * @param limit 返回数量
     * @param period 时间周期（如"day", "week", "month"）
     * @return 热门图片列表
     */
    List<PictureVO> getHotPictures(Integer limit, String period);

    /**
     * 获取用户的互动统计
     * @param userId 用户ID
     * @return 用户互动统计数据
     */
    Map<String, Long> getUserInteractionStats(Long userId);

    /**
     * 填充图片的用户点赞/收藏状态
     * @param picture 图片
     * @param userId 用户ID
     */
    void fillPictureUserReactionStatus(PictureVO picture, Long userId);

    /**
     * 批量填充图片列表的用户点赞/收藏状态
     * @param pictures 图片列表
     * @param userId 用户ID
     */
    void batchFillPictureUserReactionStatus(List<PictureVO> pictures, Long userId);
}
