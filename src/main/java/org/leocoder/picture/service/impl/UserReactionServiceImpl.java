package org.leocoder.picture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.constants.RedisConstants;
import org.leocoder.picture.domain.message.PictureReactionMessage;
import org.leocoder.picture.domain.pojo.Picture;
import org.leocoder.picture.domain.pojo.UserReaction;
import org.leocoder.picture.domain.userreaction.UserReactionRequest;
import org.leocoder.picture.domain.vo.picture.PictureVO;
import org.leocoder.picture.domain.vo.userreaction.UserReactionCountVO;
import org.leocoder.picture.domain.vo.userreaction.UserReactionStatusVO;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.exception.ThrowUtils;
import org.leocoder.picture.mapper.PictureMapper;
import org.leocoder.picture.mapper.UserReactionMapper;
import org.leocoder.picture.service.PictureService;
import org.leocoder.picture.service.UserReactionService;
import org.leocoder.picture.service.mq.MessageProducerService;
import org.leocoder.picture.utils.UserContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-16 21:53
 * @description : 用户点赞、收藏、喜欢等操作的实现类
 */

@Slf4j
@Service
public class UserReactionServiceImpl implements UserReactionService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final UserReactionMapper userReactionMapper;

    private final PictureMapper pictureMapper;

    private final PictureService pictureService;

    private final MessageProducerService messageProducerService;

    public UserReactionServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            UserReactionMapper userReactionMapper,
            PictureMapper pictureMapper,
            @Lazy PictureService pictureService,
            MessageProducerService messageProducerService) {

        this.redisTemplate = redisTemplate;
        this.userReactionMapper = userReactionMapper;
        this.pictureMapper = pictureMapper;
        this.pictureService = pictureService;
        this.messageProducerService = messageProducerService;
    }

    /**
     * 添加用户点赞/收藏（点赞、收藏等）
     *
     * @param request 点赞/收藏请求
     * @param userId  用户ID
     * @return 操作是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addReaction(UserReactionRequest request, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(request), ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(userId), ErrorCode.NO_AUTH_ERROR, "用户未登录");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getTargetType()), ErrorCode.PARAMS_ERROR, "目标类型不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(request.getTargetId()), ErrorCode.PARAMS_ERROR, "目标ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getReactionType()), ErrorCode.PARAMS_ERROR, "反应类型不能为空");

        String targetType = request.getTargetType();
        Long targetId = request.getTargetId();
        String reactionType = request.getReactionType();

        try {
            // 检查目标是否存在
            if (RedisConstants.TARGET_PICTURE.equals(targetType)) {
                Picture picture = pictureMapper.selectById(targetId);
                ThrowUtils.throwIf(ObjectUtil.isNull(picture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }

            // 检查是否已经存在相同的点赞/收藏
            UserReaction existingReaction = userReactionMapper.selectByUserAndTarget(
                    targetType, targetId, reactionType, userId);

            if (existingReaction != null) {
                // 如果已存在且已删除，则恢复（更新is_deleted为0）
                if (existingReaction.getIsDeleted() == 1) {
                    existingReaction.setIsDeleted(0);
                    existingReaction.setUpdateTime(LocalDateTime.now());
                    existingReaction.setUpdateUser(userId);
                    userReactionMapper.updateById(existingReaction);

                    // 增加目标计数
                    incrementTargetCounter(targetType, targetId, reactionType);

                    // 更新Redis缓存
                    updateReactionInCache(targetType, targetId, reactionType, userId, true);

                    return true;
                }
                // 已存在且未删除，无需重复添加
                return false;
            }

            // 创建新的点赞/收藏记录
            UserReaction userReaction = UserReaction.builder()
                    .targetType(targetType)
                    .targetId(targetId)
                    .reactionType(reactionType)
                    .createTime(LocalDateTime.now())
                    .createUser(userId)
                    .updateTime(LocalDateTime.now())
                    .updateUser(userId)
                    .isDeleted(0)
                    .build();

            // 保存到数据库
            int result = userReactionMapper.insert(userReaction);

            if (result > 0) {
                // 增加目标计数
                incrementTargetCounter(targetType, targetId, reactionType);

                // 更新Redis缓存
                updateReactionInCache(targetType, targetId, reactionType, userId, true);

                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("添加用户点赞/收藏失败: userId={}, targetType={}, targetId={}, reactionType={}",
                    userId, targetType, targetId, reactionType, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加反应失败: " + e.getMessage());
        }
    }


    /**
     * 取消用户点赞/收藏
     *
     * @param request 点赞/收藏请求
     * @param userId  用户ID
     * @return 操作是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeReaction(UserReactionRequest request, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(request), ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(userId), ErrorCode.NO_AUTH_ERROR, "用户未登录");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getTargetType()), ErrorCode.PARAMS_ERROR, "目标类型不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(request.getTargetId()), ErrorCode.PARAMS_ERROR, "目标ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getReactionType()), ErrorCode.PARAMS_ERROR, "反应类型不能为空");

        String targetType = request.getTargetType();
        Long targetId = request.getTargetId();
        String reactionType = request.getReactionType();

        try {
            // 查找用户的点赞/收藏记录
            UserReaction existingReaction = userReactionMapper.selectByUserAndTarget(
                    targetType, targetId, reactionType, userId);

            // 如果记录不存在或已删除，则无需操作
            if (existingReaction == null || existingReaction.getIsDeleted() == 1) {
                return false;
            }

            // 软删除点赞/收藏记录
            int result = userReactionMapper.softDeleteReaction(targetType, targetId, reactionType, userId);

            if (result > 0) {
                // 减少目标计数
                decrementTargetCounter(targetType, targetId, reactionType);

                // 更新Redis缓存
                updateReactionInCache(targetType, targetId, reactionType, userId, false);

                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("移除用户点赞/收藏失败: userId={}, targetType={}, targetId={}, reactionType={}",
                    userId, targetType, targetId, reactionType, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "移除点赞/收藏失败: " + e.getMessage());
        }
    }


    /**
     * 查询用户对特定目标的点赞/收藏状态
     *
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @param userId     用户ID
     * @return 点赞/收藏状态（包含是否点赞、收藏等）
     */
    @Override
    public UserReactionStatusVO getUserReactionStatus(String targetType, Long targetId, Long userId) {
        ThrowUtils.throwIf(StrUtil.isBlank(targetType), ErrorCode.PARAMS_ERROR, "目标类型不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(targetId), ErrorCode.PARAMS_ERROR, "目标ID不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(userId), ErrorCode.NO_AUTH_ERROR, "用户未登录");

        try {
            // 尝试从Redis获取缓存
            String cacheKey = RedisConstants.getUserReactionKey(targetType, targetId, userId);
            UserReactionStatusVO statusFromCache = (UserReactionStatusVO) redisTemplate.opsForValue().get(cacheKey);

            if (statusFromCache != null) {
                return statusFromCache;
            }

            // 缓存未命中，从数据库查询
            boolean hasLiked = checkReactionExists(targetType, targetId, RedisConstants.REACTION_LIKE, userId);
            boolean hasFavorited = checkReactionExists(targetType, targetId, RedisConstants.REACTION_FAVORITE, userId);

            // 构建状态对象
            UserReactionStatusVO status = UserReactionStatusVO.builder()
                    .targetId(targetId)
                    .targetType(targetType)
                    .userId(userId)
                    .hasLiked(hasLiked)
                    .hasFavorited(hasFavorited)
                    .build();

            // 缓存结果
            redisTemplate.opsForValue().set(cacheKey, status, RedisConstants.USER_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);

            return status;
        } catch (Exception e) {
            log.error("获取用户点赞/收藏状态失败: userId={}, targetType={}, targetId={}",
                    userId, targetType, targetId, e);
            // 发生异常时返回默认状态
            return UserReactionStatusVO.builder()
                    .targetId(targetId)
                    .targetType(targetType)
                    .userId(userId)
                    .hasLiked(false)
                    .hasFavorited(false)
                    .build();
        }
    }


    /**
     * 批量查询用户对多个目标的点赞/收藏状态
     *
     * @param targetType 目标类型
     * @param targetIds  目标ID列表
     * @param userId     用户ID
     * @return 目标ID到点赞/收藏状态的映射
     */
    @Override
    public Map<Long, UserReactionStatusVO> batchGetUserReactionStatus(String targetType, List<Long> targetIds, Long userId) {
        ThrowUtils.throwIf(StrUtil.isBlank(targetType), ErrorCode.PARAMS_ERROR, "目标类型不能为空");
        ThrowUtils.throwIf(CollUtil.isEmpty(targetIds), ErrorCode.PARAMS_ERROR, "目标ID列表不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(userId), ErrorCode.NO_AUTH_ERROR, "用户未登录");

        try {
            Map<Long, UserReactionStatusVO> resultMap = new HashMap<>(targetIds.size());
            List<Long> uncachedTargetIds = new ArrayList<>();

            // 尝试从Redis批量获取
            for (Long targetId : targetIds) {
                String cacheKey = RedisConstants.getUserReactionKey(targetType, targetId, userId);
                UserReactionStatusVO status = (UserReactionStatusVO) redisTemplate.opsForValue().get(cacheKey);

                if (status != null) {
                    resultMap.put(targetId, status);
                } else {
                    uncachedTargetIds.add(targetId);
                }
            }

            // 对于未缓存的目标，从数据库中查询
            if (!uncachedTargetIds.isEmpty()) {
                // 一次性查询所有未缓存目标的点赞状态
                List<UserReaction> likeReactions = userReactionMapper.selectUserReactions(
                        userId, targetType, RedisConstants.REACTION_LIKE, null, null);

                // 一次性查询所有未缓存目标的收藏状态
                List<UserReaction> favoriteReactions = userReactionMapper.selectUserReactions(
                        userId, targetType, RedisConstants.REACTION_FAVORITE, null, null);

                // 构建点赞和收藏状态的集合
                Set<Long> likedTargetIds = likeReactions.stream()
                        .filter(r -> r.getIsDeleted() == 0)
                        .map(UserReaction::getTargetId)
                        .collect(Collectors.toSet());

                Set<Long> favoritedTargetIds = favoriteReactions.stream()
                        .filter(r -> r.getIsDeleted() == 0)
                        .map(UserReaction::getTargetId)
                        .collect(Collectors.toSet());

                // 填充结果并缓存
                for (Long targetId : uncachedTargetIds) {
                    boolean hasLiked = likedTargetIds.contains(targetId);
                    boolean hasFavorited = favoritedTargetIds.contains(targetId);

                    UserReactionStatusVO status = UserReactionStatusVO.builder()
                            .targetId(targetId)
                            .targetType(targetType)
                            .userId(userId)
                            .hasLiked(hasLiked)
                            .hasFavorited(hasFavorited)
                            .build();

                    resultMap.put(targetId, status);

                    // 缓存结果
                    String cacheKey = RedisConstants.getUserReactionKey(targetType, targetId, userId);
                    redisTemplate.opsForValue().set(cacheKey, status, RedisConstants.USER_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                }
            }

            return resultMap;
        } catch (Exception e) {
            log.error("批量获取用户点赞/收藏状态失败: userId={}, targetType={}, targetIds={}",
                    userId, targetType, targetIds, e);

            // 发生异常时返回空状态映射
            Map<Long, UserReactionStatusVO> fallbackMap = new HashMap<>(targetIds.size());
            for (Long targetId : targetIds) {
                fallbackMap.put(targetId, UserReactionStatusVO.builder()
                        .targetId(targetId)
                        .targetType(targetType)
                        .userId(userId)
                        .hasLiked(false)
                        .hasFavorited(false)
                        .build());
            }
            return fallbackMap;
        }
    }


    /**
     * 获取目标的点赞/收藏计数
     *
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 点赞/收藏计数（包含点赞数、收藏数等）
     */
    @Override
    public UserReactionCountVO getReactionCounts(String targetType, Long targetId) {
        ThrowUtils.throwIf(StrUtil.isBlank(targetType), ErrorCode.PARAMS_ERROR, "目标类型不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(targetId), ErrorCode.PARAMS_ERROR, "目标ID不能为空");

        try {
            // 构建缓存键
            String likeCountKey = RedisConstants.getReactionCountKey(targetType, targetId, RedisConstants.REACTION_LIKE);
            String favoriteCountKey = RedisConstants.getReactionCountKey(targetType, targetId, RedisConstants.REACTION_FAVORITE);
            String viewCountKey = RedisConstants.getReactionCountKey(targetType, targetId, RedisConstants.REACTION_VIEW);

            // 尝试从Redis获取缓存
            Long likeCount = getLongFromRedis(likeCountKey);
            Long favoriteCount = getLongFromRedis(favoriteCountKey);
            Long viewCount = getLongFromRedis(viewCountKey);

            boolean allCached = likeCount != null && favoriteCount != null && viewCount != null;

            // 如果缓存不完整，从数据库加载
            if (!allCached) {
                if (RedisConstants.TARGET_PICTURE.equals(targetType)) {
                    Picture picture = pictureMapper.selectById(targetId);
                    if (picture != null) {
                        likeCount = picture.getLikeCount() != null ? picture.getLikeCount().longValue() : 0L;
                        favoriteCount = picture.getCollectionCount() != null ? picture.getCollectionCount().longValue() : 0L;
                        viewCount = picture.getViewCount() != null ? picture.getViewCount() : 0L;

                        // 更新缓存
                        redisTemplate.opsForValue().set(likeCountKey, likeCount, RedisConstants.COUNT_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                        redisTemplate.opsForValue().set(favoriteCountKey, favoriteCount, RedisConstants.COUNT_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                        redisTemplate.opsForValue().set(viewCountKey, viewCount, RedisConstants.COUNT_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                    }
                } else {
                    // 对于非图片类型，从用户点赞/收藏表中统计
                    likeCount = userReactionMapper.countReactionsByTarget(targetType, targetId, RedisConstants.REACTION_LIKE);
                    favoriteCount = userReactionMapper.countReactionsByTarget(targetType, targetId, RedisConstants.REACTION_FAVORITE);
                    viewCount = userReactionMapper.countReactionsByTarget(targetType, targetId, RedisConstants.REACTION_VIEW);

                    // 更新缓存
                    redisTemplate.opsForValue().set(likeCountKey, likeCount, RedisConstants.COUNT_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                    redisTemplate.opsForValue().set(favoriteCountKey, favoriteCount, RedisConstants.COUNT_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                    redisTemplate.opsForValue().set(viewCountKey, viewCount, RedisConstants.COUNT_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                }
            }

            // 构建返回对象
            return UserReactionCountVO.builder()
                    .targetId(targetId)
                    .targetType(targetType)
                    .likeCount(likeCount != null ? likeCount : 0L)
                    .favoriteCount(favoriteCount != null ? favoriteCount : 0L)
                    .viewCount(viewCount != null ? viewCount : 0L)
                    .build();
        } catch (Exception e) {
            log.error("获取点赞/收藏计数失败: targetType={}, targetId={}", targetType, targetId, e);
            // 发生异常时返回默认计数
            return UserReactionCountVO.builder()
                    .targetId(targetId)
                    .targetType(targetType)
                    .likeCount(0L)
                    .favoriteCount(0L)
                    .viewCount(0L)
                    .build();
        }
    }

    @Override
    public Map<Long, UserReactionCountVO> batchGetReactionCounts(String targetType, List<Long> targetIds) {
        ThrowUtils.throwIf(StrUtil.isBlank(targetType), ErrorCode.PARAMS_ERROR, "目标类型不能为空");
        ThrowUtils.throwIf(CollUtil.isEmpty(targetIds), ErrorCode.PARAMS_ERROR, "目标ID列表不能为空");

        try {
            Map<Long, UserReactionCountVO> resultMap = new HashMap<>(targetIds.size());

            if (RedisConstants.TARGET_PICTURE.equals(targetType)) {
                // 对于图片类型，直接查询图片表
                // 这里假设你有一个方法可以批量查询图片
                List<Picture> pictures = new ArrayList<>();
                for (Long targetId : targetIds) {
                    Picture picture = pictureMapper.selectById(targetId);
                    if (picture != null) {
                        pictures.add(picture);
                    }
                }

                // 处理查询结果
                for (Picture picture : pictures) {
                    Long likeCount = picture.getLikeCount() != null ? picture.getLikeCount().longValue() : 0L;
                    Long favoriteCount = picture.getCollectionCount() != null ? picture.getCollectionCount().longValue() : 0L;
                    Long viewCount = picture.getViewCount() != null ? picture.getViewCount() : 0L;

                    UserReactionCountVO countVO = UserReactionCountVO.builder()
                            .targetId(picture.getId())
                            .targetType(targetType)
                            .likeCount(likeCount)
                            .favoriteCount(favoriteCount)
                            .viewCount(viewCount)
                            .build();

                    resultMap.put(picture.getId(), countVO);

                    // 更新缓存
                    updateCountCache(targetType, picture.getId(), likeCount, favoriteCount, viewCount);
                }
            } else {
                // 对于非图片类型，从用户点赞/收藏表中统计
                for (Long targetId : targetIds) {
                    Long likeCount = userReactionMapper.countReactionsByTarget(targetType, targetId, RedisConstants.REACTION_LIKE);
                    Long favoriteCount = userReactionMapper.countReactionsByTarget(targetType, targetId, RedisConstants.REACTION_FAVORITE);
                    Long viewCount = userReactionMapper.countReactionsByTarget(targetType, targetId, RedisConstants.REACTION_VIEW);

                    UserReactionCountVO countVO = UserReactionCountVO.builder()
                            .targetId(targetId)
                            .targetType(targetType)
                            .likeCount(likeCount != null ? likeCount : 0L)
                            .favoriteCount(favoriteCount != null ? favoriteCount : 0L)
                            .viewCount(viewCount != null ? viewCount : 0L)
                            .build();

                    resultMap.put(targetId, countVO);

                    // 更新缓存
                    updateCountCache(targetType, targetId, likeCount, favoriteCount, viewCount);
                }
            }

            return resultMap;
        } catch (Exception e) {
            log.error("批量获取点赞/收藏计数失败: targetType={}, targetIds={}", targetType, targetIds, e);

            // 发生异常时返回空计数映射
            Map<Long, UserReactionCountVO> fallbackMap = new HashMap<>(targetIds.size());
            for (Long targetId : targetIds) {
                fallbackMap.put(targetId, UserReactionCountVO.builder()
                        .targetId(targetId)
                        .targetType(targetType)
                        .likeCount(0L)
                        .favoriteCount(0L)
                        .viewCount(0L)
                        .build());
            }
            return fallbackMap;
        }
    }

    @Override
    public List<PictureVO> getHotPictures(Integer limit, String period) {
        // 参数校验
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        if (limit > 100) {
            limit = 100;
        }
        if (StrUtil.isBlank(period)) {
            period = "day";
        }

        try {
            // 尝试从缓存获取
            String cacheKey = RedisConstants.getHotPicturesKey(period);
            List<PictureVO> cachedList = (List<PictureVO>) redisTemplate.opsForValue().get(cacheKey);

            if (cachedList != null && !cachedList.isEmpty()) {
                // 返回请求数量的热门图片
                return cachedList.size() > limit ? cachedList.subList(0, limit) : cachedList;
            }

            // 缓存未命中，生成热门图片列表
            // 1. 获取热门图片ID（基于综合点赞和收藏数）
            List<Long> hotPictureIds = userReactionMapper.selectHotTargetIds(RedisConstants.TARGET_PICTURE, RedisConstants.REACTION_LIKE, limit * 2);

            // 如果点赞热门不足，补充收藏热门
            if (hotPictureIds.size() < limit) {
                List<Long> hotFavoriteIds = userReactionMapper.selectHotTargetIds(RedisConstants.TARGET_PICTURE, RedisConstants.REACTION_FAVORITE, limit * 2);
                // 合并并去重
                for (Long id : hotFavoriteIds) {
                    if (!hotPictureIds.contains(id)) {
                        hotPictureIds.add(id);
                        if (hotPictureIds.size() >= limit * 2) {
                            break;
                        }
                    }
                }
            }

            if (hotPictureIds.isEmpty()) {
                return new ArrayList<>();
            }

            // 2. 查询图片详情
            List<PictureVO> hotPictures = new ArrayList<>();
            for (Long pictureId : hotPictureIds) {
                try {
                    // 使用现有的图片服务获取详情
                    PictureVO pictureVO = pictureService.getPictureById(pictureId, UserContext.getUser());
                    if (pictureVO != null) {
                        hotPictures.add(pictureVO);
                        if (hotPictures.size() >= limit) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取热门图片详情失败: pictureId={}", pictureId, e);
                }
            }

            // 3. 缓存结果
            if (!hotPictures.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, hotPictures, RedisConstants.HOT_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            }

            return hotPictures;
        } catch (Exception e) {
            log.error("获取热门图片失败: limit={}, period={}", limit, period, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Long> getUserInteractionStats(Long userId) {
        ThrowUtils.throwIf(ObjectUtil.isNull(userId), ErrorCode.PARAMS_ERROR, "用户ID不能为空");

        try {
            // 统计用户的各类互动数据
            Map<String, Long> stats = new HashMap<>();

            // 点赞数
            Long likesGiven = userReactionMapper.countReactionsByTarget(null, null, RedisConstants.REACTION_LIKE);
            stats.put("likesGiven", likesGiven != null ? likesGiven : 0L);

            // 收藏数
            Long favoritesCreated = userReactionMapper.countReactionsByTarget(null, null, RedisConstants.REACTION_FAVORITE);
            stats.put("favoritesCreated", favoritesCreated != null ? favoritesCreated : 0L);

            // 用户图片被点赞数
            // 假设有一个方法可以查询用户上传的所有图片的点赞总数
            Long likesReceived = calculateUserPicturesLikeCount(userId);
            stats.put("likesReceived", likesReceived);

            // 用户图片被收藏数
            Long favoritesReceived = calculateUserPicturesFavoriteCount(userId);
            stats.put("favoritesReceived", favoritesReceived);

            return stats;
        } catch (Exception e) {
            log.error("获取用户互动统计失败: userId={}", userId, e);
            // 返回默认统计
            Map<String, Long> defaultStats = new HashMap<>();
            defaultStats.put("likesGiven", 0L);
            defaultStats.put("favoritesCreated", 0L);
            defaultStats.put("likesReceived", 0L);
            defaultStats.put("favoritesReceived", 0L);
            return defaultStats;
        }
    }


    /**
     * 填充图片的用户点赞/收藏状态
     *
     * @param picture 图片
     * @param userId  用户ID
     */
    @Override
    public void fillPictureUserReactionStatus(PictureVO picture, Long userId) {
        if (picture == null || userId == null) {
            return;
        }

        try {
            UserReactionStatusVO status = getUserReactionStatus(RedisConstants.TARGET_PICTURE, picture.getId(), userId);
            picture.setHasLiked(status.isHasLiked());
            picture.setHasCollectioned(status.isHasFavorited());
        } catch (Exception e) {
            log.error("填充图片用户点赞/收藏状态失败: pictureId={}, userId={}", picture.getId(), userId, e);
            // 默认为未点赞/收藏状态
            picture.setHasLiked(false);
            picture.setHasCollectioned(false);
        }
    }


    /**
     * 批量填充图片列表的用户点赞/收藏状态
     *
     * @param pictures 图片列表
     * @param userId   用户ID
     */
    @Override
    public void batchFillPictureUserReactionStatus(List<PictureVO> pictures, Long userId) {
        if (CollUtil.isEmpty(pictures) || userId == null) {
            return;
        }

        try {
            // 提取所有图片ID
            List<Long> pictureIds = pictures.stream()
                    .map(PictureVO::getId)
                    .collect(Collectors.toList());

            // 批量获取点赞/收藏状态
            Map<Long, UserReactionStatusVO> statusMap = batchGetUserReactionStatus(RedisConstants.TARGET_PICTURE, pictureIds, userId);

            // 填充点赞/收藏状态到图片
            for (PictureVO picture : pictures) {
                UserReactionStatusVO status = statusMap.get(picture.getId());
                if (status != null) {
                    picture.setHasLiked(status.isHasLiked());
                    picture.setHasCollectioned(status.isHasFavorited());
                } else {
                    // 如果没有找到状态，默认为未点赞/未收藏
                    picture.setHasLiked(false);
                    picture.setHasCollectioned(false);
                }
            }
        } catch (Exception e) {
            log.error("批量填充图片用户点赞/收藏状态失败: pictureCount={}, userId={}", pictures.size(), userId, e);
            // 默认为未点赞/收藏状态
            for (PictureVO picture : pictures) {
                picture.setHasLiked(false);
                picture.setHasCollectioned(false);
            }
        }
    }

    // ====================== 辅助方法 ======================

    /**
     * rocketmq异步计数
     *
     * @param targetType   目标类型
     * @param targetId     目标ID
     * @param reactionType 点赞/收藏类型
     */
    private void incrementTargetCounter(String targetType, Long targetId, String reactionType) {
        // 更新Redis中的计数 - 立即执行，保证缓存一致性
        String countKey = RedisConstants.getReactionCountKey(targetType, targetId, reactionType);
        redisTemplate.opsForValue().increment(countKey, 1);
        redisTemplate.expire(countKey, RedisConstants.COUNT_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);

        // 只有图片类型的操作才需要异步更新数据库
        if (RedisConstants.TARGET_PICTURE.equals(targetType)) {
            // 构建消息并发送到RocketMQ
            PictureReactionMessage message = PictureReactionMessage.builder()
                    .pictureId(targetId)
                    .reactionType(reactionType)
                    .operationType("add")
                    .userId(UserContext.getUserId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            // 发送消息到RocketMQ
            boolean sendResult = messageProducerService.sendPictureReactionMessage(message);

            if (!sendResult) {
                log.warn("发送增加计数消息失败，降级为同步更新: targetType={}, targetId={}, reactionType={}",
                        targetType, targetId, reactionType);
                // 降级为同步更新
                if (RedisConstants.REACTION_LIKE.equals(reactionType)) {
                    pictureMapper.incrementLikeCount(targetId);
                } else if (RedisConstants.REACTION_FAVORITE.equals(reactionType)) {
                    pictureMapper.incrementCollectionCount(targetId);
                }
            }
        }
    }

    /**
     * rocketmq异步减数
     *
     * @param targetType   目标类型
     * @param targetId     目标ID
     * @param reactionType 点赞/收藏类型
     */
    private void decrementTargetCounter(String targetType, Long targetId, String reactionType) {
        // 更新Redis中的计数 - 立即执行，保证缓存一致性
        String countKey = RedisConstants.getReactionCountKey(targetType, targetId, reactionType);
        Object countObj = redisTemplate.opsForValue().get(countKey);
        Long count = getLongFromRedis(countKey);

        if (count != null && count > 0) {
            redisTemplate.opsForValue().decrement(countKey, 1);
        }

        // 只有图片类型的操作才需要异步更新数据库
        if (RedisConstants.TARGET_PICTURE.equals(targetType)) {
            // 构建消息并发送到RocketMQ
            PictureReactionMessage message = PictureReactionMessage.builder()
                    .pictureId(targetId)
                    .reactionType(reactionType)
                    .operationType("remove")
                    .userId(UserContext.getUserId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            // 发送消息到RocketMQ
            boolean sendResult = messageProducerService.sendPictureReactionMessage(message);

            if (!sendResult) {
                log.warn("发送减少计数消息失败，降级为同步更新: targetType={}, targetId={}, reactionType={}",
                        targetType, targetId, reactionType);
                // 降级为同步更新
                if (RedisConstants.REACTION_LIKE.equals(reactionType)) {
                    pictureMapper.decrementLikeCount(targetId);
                } else if (RedisConstants.REACTION_FAVORITE.equals(reactionType)) {
                    pictureMapper.decrementCollectionCount(targetId);
                }
            }
        }
    }

    /**
     * 更新缓存中的点赞/收藏状态
     */
    private void updateReactionInCache(String targetType, Long targetId, String reactionType, Long userId, boolean isAdd) {
        String userKey = RedisConstants.getUserReactionKey(targetType, targetId, userId);
        UserReactionStatusVO cachedStatus = (UserReactionStatusVO) redisTemplate.opsForValue().get(userKey);

        if (cachedStatus != null) {
            // 更新状态
            if (RedisConstants.REACTION_LIKE.equals(reactionType)) {
                cachedStatus.setHasLiked(isAdd);
            } else if (RedisConstants.REACTION_FAVORITE.equals(reactionType)) {
                cachedStatus.setHasFavorited(isAdd);
            }

            // 重新设置缓存
            redisTemplate.opsForValue().set(userKey, cachedStatus, RedisConstants.USER_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        } else {
            // 创建新的状态对象
            UserReactionStatusVO newStatus = UserReactionStatusVO.builder()
                    .targetId(targetId)
                    .targetType(targetType)
                    .userId(userId)
                    .hasLiked(RedisConstants.REACTION_LIKE.equals(reactionType) && isAdd)
                    .hasFavorited(RedisConstants.REACTION_FAVORITE.equals(reactionType) && isAdd)
                    .build();

            redisTemplate.opsForValue().set(userKey, newStatus, RedisConstants.USER_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        }

        // 更新计数缓存过期时间
        String countKey = RedisConstants.getReactionCountKey(targetType, targetId, reactionType);
        redisTemplate.expire(countKey, RedisConstants.COUNT_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);

        // 清除热门图片缓存（让新的热门图片可以及时反映）
        clearHotPicturesCache();
    }

    /**
     * 更新计数缓存
     */
    private void updateCountCache(String targetType, Long targetId, Long likeCount, Long favoriteCount, Long viewCount) {
        String likeCountKey = RedisConstants.getReactionCountKey(targetType, targetId, RedisConstants.REACTION_LIKE);
        String favoriteCountKey = RedisConstants.getReactionCountKey(targetType, targetId, RedisConstants.REACTION_FAVORITE);
        String viewCountKey = RedisConstants.getReactionCountKey(targetType, targetId, RedisConstants.REACTION_VIEW);

        redisTemplate.opsForValue().set(likeCountKey, likeCount, RedisConstants.COUNT_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(favoriteCountKey, favoriteCount, RedisConstants.COUNT_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(viewCountKey, viewCount, RedisConstants.COUNT_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    /**
     * 清除热门图片缓存
     */
    private void clearHotPicturesCache() {
        try {
            Set<String> keys = redisTemplate.keys(RedisConstants.HOT_PICTURES_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.error("清除热门图片缓存失败", e);
        }
    }

    /**
     * 检查用户是否对目标有特定类型的点赞/收藏
     */
    private boolean checkReactionExists(String targetType, Long targetId, String reactionType, Long userId) {
        UserReaction reaction = userReactionMapper.selectByUserAndTarget(targetType, targetId, reactionType, userId);
        return reaction != null && reaction.getIsDeleted() == 0;
    }

    /**
     * 从Redis获取Long值
     */
    private Long getLongFromRedis(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 计算用户图片被点赞总数
     */
    private Long calculateUserPicturesLikeCount(Long userId) {
        // 查询用户上传的所有图片
        List<Picture> userPictures = new ArrayList<>();

        // 计算总点赞数
        return userPictures.stream()
                .mapToLong(picture -> picture.getLikeCount() != null ? picture.getLikeCount() : 0)
                .sum();
    }

    /**
     * 计算用户图片被收藏总数
     */
    private Long calculateUserPicturesFavoriteCount(Long userId) {
        // 查询用户上传的所有图片
        List<Picture> userPictures = new ArrayList<>();

        // 计算总收藏数
        return userPictures.stream()
                .mapToLong(picture -> picture.getCollectionCount() != null ? picture.getCollectionCount() : 0)
                .sum();
    }
}