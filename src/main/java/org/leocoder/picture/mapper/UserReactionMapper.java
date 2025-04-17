package org.leocoder.picture.mapper;

import org.apache.ibatis.annotations.Param;
import org.leocoder.picture.domain.pojo.UserReaction;

import java.util.List;

/**
 * @author : 程序员Leo
 * @date  2025-04-16 21:53
 * @version 1.0
 * @description : 用户点赞、踩、收藏、查看等操作数据访问层
 */

public interface UserReactionMapper {
    int deleteById(Long id);

    int insert(UserReaction record);

    int insertWithId(UserReaction record);

    int insertSelective(UserReaction record);

    UserReaction selectById(Long id);

    int updateByPrimaryKeySelective(UserReaction record);

    int updateById(UserReaction record);

    // 根据用户ID、目标类型、目标ID和点赞/收藏类型查询用户点赞/收藏
    UserReaction selectByUserAndTarget(@Param("targetType") String targetType,
                                       @Param("targetId") Long targetId,
                                       @Param("reactionType") String reactionType,
                                       @Param("createUser") Long createUser);

    // 软删除用户点赞/收藏（设置is_deleted=1）
    int softDeleteReaction(@Param("targetType") String targetType,
                           @Param("targetId") Long targetId,
                           @Param("reactionType") String reactionType,
                           @Param("createUser") Long createUser);

    // 统计目标的特定点赞/收藏数量
    Long countReactionsByTarget(@Param("targetType") String targetType,
                                @Param("targetId") Long targetId,
                                @Param("reactionType") String reactionType);

    // 批量查询多个目标的特定点赞/收藏数量
    List<UserReaction> countBatchReactionsByTargets(@Param("targetType") String targetType,
                                                    @Param("targetIds") List<Long> targetIds,
                                                    @Param("reactionType") String reactionType);

    // 查询用户所有的点赞/收藏记录（按目标类型和点赞/收藏类型）
    List<UserReaction> selectUserReactions(@Param("createUser") Long userId,
                                           @Param("targetType") String targetType,
                                           @Param("reactionType") String reactionType,
                                           @Param("limit") Integer limit,
                                           @Param("offset") Integer offset);

    // 获取热门内容ID列表（基于特定点赞/收藏类型）
    List<Long> selectHotTargetIds(@Param("targetType") String targetType,
                                  @Param("reactionType") String reactionType,
                                  @Param("limit") Integer limit);
}