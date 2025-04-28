package org.leocoder.picture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.leocoder.picture.cache.PictureCacheManager;
import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.common.PageUtils;
import org.leocoder.picture.constant.RedisKeyConstants;
import org.leocoder.picture.constants.RedisConstants;
import org.leocoder.picture.domain.dto.picture.*;
import org.leocoder.picture.domain.dto.upload.UploadPictureResult;
import org.leocoder.picture.domain.mapstruct.PictureConvert;
import org.leocoder.picture.domain.mapstruct.UserConvert;
import org.leocoder.picture.domain.message.PictureReactionMessage;
import org.leocoder.picture.domain.pojo.Category;
import org.leocoder.picture.domain.pojo.Picture;
import org.leocoder.picture.domain.pojo.PictureHash;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.picture.PictureStatisticsVO;
import org.leocoder.picture.domain.vo.picture.PictureVO;
import org.leocoder.picture.domain.vo.picture.PictureWaterfallVO;
import org.leocoder.picture.domain.vo.user.UserVO;
import org.leocoder.picture.enums.PictureReviewStatusEnum;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.exception.ThrowUtils;
import org.leocoder.picture.manager.crawler.PictureCrawler;
import org.leocoder.picture.manager.crawler.PictureCrawlerManager;
import org.leocoder.picture.manager.upload.FilePictureUpload;
import org.leocoder.picture.manager.upload.PictureUploadTemplate;
import org.leocoder.picture.manager.upload.UrlPictureUpload;
import org.leocoder.picture.mapper.*;
import org.leocoder.picture.service.*;
import org.leocoder.picture.service.mq.MessageProducerService;
import org.leocoder.picture.utils.SnowflakeIdGenerator;
import org.leocoder.picture.utils.UserContext;
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
 * @date 2025-04-11 00:36
 * @description : 图片服务实现 (优化版)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PictureServiceImpl implements PictureService {

    private final UserService userService;

    private final UserMapper userMapper;

    private final PictureMapper pictureMapper;

    private final FilePictureUpload filePictureUpload;

    private final UrlPictureUpload urlPictureUpload;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private final PictureCrawlerManager pictureCrawlerManager;

    private final TagRelationService tagRelationService;

    private final CategoryRelationService categoryRelationService;

    private final CategoryMapper categoryMapper;

    private final TagMapper tagMapper;

    private final CategoryService categoryService;

    private final TagService tagService;

    private final PictureHashMapper pictureHashMapper;

    private final UserReactionService userReactionService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final MessageProducerService messageProducerService;

    private final PictureCacheManager pictureCacheManager;

    /**
     * 填充审核参数
     *
     * @param picture   图片信息
     * @param loginUser 登录用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewUserId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(LocalDateTime.now());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
            picture.setReviewMessage("等待管理员审核 ，。。");
        }
    }

    /**
     * 上传图片
     *
     * @param inputSource  来源对象（URL上传或者文件上传）
     * @param requestParam 请求参数
     * @param loginUser    登录用户
     * @return PictureVO
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest requestParam, User loginUser) {
        // 基础校验
        ThrowUtils.throwIf(ObjectUtil.isEmpty(inputSource), ErrorCode.PARAMS_ERROR, "图片为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 确定是新增还是更新
        Long pictureId = ObjectUtil.isNotEmpty(requestParam) ? requestParam.getId() : null;

        // 更新图片时的权限校验
        if (pictureId != null) {
            Picture oldPicture = pictureMapper.selectById(pictureId);
            ThrowUtils.throwIf(ObjectUtil.isNull(oldPicture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");

            // 仅本人或管理员可编辑
            boolean canEdit = oldPicture.getCreateUser().equals(loginUser.getId()) || userService.isAdmin(loginUser);
            ThrowUtils.throwIf(!canEdit, ErrorCode.NO_AUTH_ERROR);
        }

        // 执行上传
        String uploadPathPrefix = String.format("gallery-public/%s", loginUser.getId());
        PictureUploadTemplate pictureUploadTemplate = inputSource instanceof String ? urlPictureUpload : filePictureUpload;
        UploadPictureResult uploadResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构建并填充图片信息
        Picture picture = buildPicture(loginUser, uploadResult, pictureId, requestParam);
        fillReviewParams(picture, loginUser);

        try {
            // 保存或更新图片信息
            boolean result;
            if (pictureId != null) {
                // 更新操作
                picture.setUpdateTime(LocalDateTime.now());
                picture.setUpdateUser(loginUser.getId());
                result = pictureMapper.updateByPrimaryKeySelective(picture) > 0;
            } else {
                // 生成雪花算法ID
                Long snowflakeId = snowflakeIdGenerator.nextId();
                picture.setId(snowflakeId);

                picture.setCreateTime(LocalDateTime.now());
                picture.setIsDeleted(0);

                // 使用insertWithId方法插入记录（保留ID）
                result = pictureMapper.insertWithId(picture) > 0;

                log.info("使用雪花算法生成图片ID: {}", snowflakeId);
            }

            // 查询插入后的记录以验证字段是否正确保存
            if (result) {
                Picture savedPicture = pictureMapper.selectById(picture.getId());
                log.info("保存后的图片信息: id={}, format={}, size={}, originalName={}",
                        savedPicture.getId(), savedPicture.getFormat(), savedPicture.getSize(), savedPicture.getOriginalName());
            }

            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");

            // 转换为VO并记录日志
            PictureVO pictureVO = PictureVO.objToVo(picture);
            log.info("转换为VO后的图片信息: id={}, format={}, size={}",
                    pictureVO.getId(), pictureVO.getFormat(), pictureVO.getSize());

            // 上传完成后清除相关缓存
            pictureCacheManager.invalidateAfterPictureUpload(loginUser.getId());

            return pictureVO;
        } catch (Exception e) {
            log.error("保存图片信息失败", e);
            throw new RuntimeException("保存图片信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建图片实体
     * 确保所有必要的属性都从uploadResult正确设置到Picture对象中
     */
    private Picture buildPicture(User loginUser, UploadPictureResult uploadResult, Long pictureId, PictureUploadRequest requestParam) {
        // 使用建造者模式创建Picture对象，确保所有属性正确设置
        Picture picture = Picture.builder()
                // 如果是更新操作则使用原ID，否则在上传方法中使用雪花算法生成ID
                .id(pictureId)
                .url(uploadResult.getUrl())
                .thumbnailUrl(uploadResult.getThumbnailUrl())
                // 设置原始文件名
                .originalName(uploadResult.getOriginalName())
                .name(ObjectUtil.isNotEmpty(requestParam) && StrUtil.isNotBlank(requestParam.getPicName())
                        ? requestParam.getPicName() : uploadResult.getPicName())
                // 设置图片大小
                .size(uploadResult.getPicSize())
                .picWidth(uploadResult.getPicWidth())
                .picHeight(uploadResult.getPicHeight())
                .picScale(uploadResult.getPicScale())
                // 设置图片格式
                .format(uploadResult.getPicFormat())
                .mainColor(uploadResult.getPicColor())
                .createUser(loginUser.getId())
                .viewCount(0L)
                .likeCount(0)
                .collectionCount(0)
                .build();

        // 日志记录，帮助调试属性设置情况
        log.info("构建图片实体: id={}, originalName={}, format={}, size={}",
                picture.getId(), picture.getOriginalName(), picture.getFormat(), picture.getSize());

        return picture;
    }


    /**
     * 根据ID获取图片详情
     *
     * @param id        图片ID
     * @param loginUser 当前登录用户
     * @return 图片详情
     */
    @Override
    public PictureVO getPictureById(Long id, User loginUser) {
        ThrowUtils.throwIf(ObjectUtil.isNull(id), ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR);

        // 从数据库查询图片
        Picture picture = pictureMapper.selectById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(picture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 检查图片是否已审核通过或者是当前用户的图片或者是管理员
        boolean canView = picture.getReviewStatus() == PictureReviewStatusEnum.PASS.getValue()
                || picture.getCreateUser().equals(loginUser.getId())
                || userService.isAdmin(loginUser);

        ThrowUtils.throwIf(!canView, ErrorCode.NO_AUTH_ERROR, "您无权查看此图片");

        // 增加浏览量
        incrementViewCount(id);

        // 转换为VO对象
        PictureVO pictureVO = PictureVO.objToVo(picture);

        // 设置图片创建的用户信息
        pictureVO.setUser(userService.getUserById(picture.getCreateUser()));

        try {
            // 获取并设置图片关联的分类信息 - 一张图片只关联一个分类
            List<Long> categoryIds = categoryRelationService.getCategoryIdsByContent("picture", id);
            if (CollUtil.isNotEmpty(categoryIds)) {
                // 我们取列表中的第一个分类ID
                Long categoryId = categoryIds.get(0);

                // 设置分类ID
                pictureVO.setCategoryId(String.valueOf(categoryId));

                Category category = categoryMapper.selectById(categoryId);
                if (category != null) {
                    // 设置分类名称
                    pictureVO.setCategory(category.getName());

                    log.debug("图片分类信息: 图片ID={}, 分类ID={}, 分类名称={}, 分类图标={}", id, categoryId, category.getName());
                }
            }

            // 获取并设置图片关联的标签信息 - 一张图片可以关联多个标签
            List<Long> tagIds = tagRelationService.getTagIdsByContent("picture", id);
            if (CollUtil.isNotEmpty(tagIds)) {
                // 将所有标签ID转换为字符串列表
                List<String> tagIdStrings = tagIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                pictureVO.setTagIds(tagIdStrings);

                // 获取标签名称列表
                List<String> tagNames = tagMapper.selectNamesByIds(tagIds);
                pictureVO.setTags(tagNames);

                // 获取所有标签的颜色列表
                List<Map<String, Object>> tagColorMaps = tagMapper.selectColorsByIds(tagIds);
                if (CollUtil.isNotEmpty(tagColorMaps)) {
                    List<String> tagColors = tagColorMaps.stream()
                            .map(map -> (String) map.get("color"))
                            .collect(Collectors.toList());
                    pictureVO.setTagColors(tagColors);
                    log.debug("设置图片标签颜色: 图片ID={}, 标签数量={}, 颜色数量={}",
                            id, tagIds.size(), tagColors.size());
                }
            }

            if (ObjectUtil.isNotNull(loginUser)) {
                try {
                    userReactionService.fillPictureUserReactionStatus(pictureVO, loginUser.getId());
                } catch (Exception e) {
                    log.warn("填充图片互动信息失败: {}", e.getMessage());
                }
            }

            log.info("加载图片关联信息成功: 图片ID={}, 分类ID={}, 分类名称={}, 标签数={}",
                    id, pictureVO.getCategoryId(), pictureVO.getCategory(),
                    pictureVO.getTags() != null ? pictureVO.getTags().size() : 0);
        } catch (Exception e) {
            log.error("加载图片关联信息失败: 图片ID={}", id, e);
            // 即使关联信息加载失败，我们仍然返回基本的图片信息
        }

        return pictureVO;
    }

    /**
     * 获取上一张图片
     *
     * @param currentPictureId 当前图片ID
     * @param loginUser        当前登录用户
     * @return 上一张图片详情，如果没有则返回null
     */
    @Override
    public PictureVO getPreviousPicture(Long currentPictureId, User loginUser) {
        ThrowUtils.throwIf(currentPictureId == null, ErrorCode.PARAMS_ERROR, "当前图片ID不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        try {
            // 获取当前图片
            Picture currentPicture = pictureMapper.selectById(currentPictureId);
            ThrowUtils.throwIf(currentPicture == null, ErrorCode.NOT_FOUND_ERROR, "当前图片不存在");

            // 查询上一张图片（ID小于当前图片且已审核通过的图片，按ID降序取第一个）
            Picture previousPicture = pictureMapper.selectPreviousPicture(
                    currentPictureId,
                    String.valueOf(PictureReviewStatusEnum.PASS.getValue())
            );

            if (previousPicture == null) {
                // 如果没有上一张图片，可以考虑返回最大ID的图片（形成循环）
                // 如果不需要循环，则直接返回null
                log.info("未找到ID小于{}的上一张图片", currentPictureId);
                return null;
            }

            // 增加浏览量
            incrementViewCount(previousPicture.getId());

            // 转换为VO并返回
            PictureVO pictureVO = PictureVO.objToVo(previousPicture);

            // 增强：添加创建用户信息
            pictureVO.setUser(userService.getUserById(previousPicture.getCreateUser()));

            // 增强：添加分类信息
            try {
                List<Long> categoryIds = categoryRelationService.getCategoryIdsByContent("picture", previousPicture.getId());
                if (CollUtil.isNotEmpty(categoryIds)) {
                    Long categoryId = categoryIds.get(0);
                    pictureVO.setCategoryId(String.valueOf(categoryId));
                    String categoryName = categoryMapper.selectById(categoryId).getName();
                    pictureVO.setCategory(categoryName);
                }
            } catch (Exception e) {
                log.warn("获取上一张图片分类信息失败: {}", e.getMessage());
            }

            // 增强：添加标签信息
            try {
                List<Long> tagIds = tagRelationService.getTagIdsByContent("picture", previousPicture.getId());
                if (CollUtil.isNotEmpty(tagIds)) {
                    List<String> tagIdStrings = tagIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList());
                    pictureVO.setTagIds(tagIdStrings);

                    List<String> tagNames = tagMapper.selectNamesByIds(tagIds);
                    pictureVO.setTags(tagNames);

                    // 获取所有标签的颜色列表
                    List<Map<String, Object>> tagColorMaps = tagMapper.selectColorsByIds(tagIds);
                    if (CollUtil.isNotEmpty(tagColorMaps)) {
                        List<String> tagColors = tagColorMaps.stream()
                                .map(map -> (String) map.get("color"))
                                .collect(Collectors.toList());
                        pictureVO.setTagColors(tagColors);
                        log.debug("设置图片标签颜色: 图片ID={}, 标签数量={}, 颜色数量={}",
                                previousPicture.getId(), tagIds.size(), tagColors.size());
                    }
                }
            } catch (Exception e) {
                log.warn("获取上一张图片标签信息失败: {}", e.getMessage());
            }

            if (ObjectUtil.isNotNull(loginUser)) {
                try {
                    userReactionService.fillPictureUserReactionStatus(pictureVO, loginUser.getId());
                } catch (Exception e) {
                    log.warn("填充图片互动信息失败: {}", e.getMessage());
                }
            }
            log.info("获取上一张图片成功: 当前图片ID={}, 上一张图片ID={}", currentPictureId, previousPicture.getId());
            return pictureVO;
        } catch (Exception e) {
            log.error("获取上一张图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取上一张图片失败: " + e.getMessage());
        }
    }

    /**
     * 获取下一张图片
     *
     * @param currentPictureId 当前图片ID
     * @param loginUser        当前登录用户
     * @return 下一张图片详情，如果没有则返回null
     */
    @Override
    public PictureVO getNextPicture(Long currentPictureId, User loginUser) {
        ThrowUtils.throwIf(currentPictureId == null, ErrorCode.PARAMS_ERROR, "当前图片ID不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        try {
            // 获取当前图片
            Picture currentPicture = pictureMapper.selectById(currentPictureId);
            ThrowUtils.throwIf(currentPicture == null, ErrorCode.NOT_FOUND_ERROR, "当前图片不存在");

            // 查询下一张图片（ID大于当前图片且已审核通过的图片，按ID升序取第一个）
            Picture nextPicture = pictureMapper.selectNextPicture(
                    currentPictureId,
                    String.valueOf(PictureReviewStatusEnum.PASS.getValue())
            );

            if (nextPicture == null) {
                // 如果没有下一张图片，可以考虑返回最小ID的图片（形成循环）
                // 如果不需要循环，则直接返回null
                log.info("未找到ID大于{}的下一张图片", currentPictureId);
                return null;
            }

            // 增加浏览量
            incrementViewCount(nextPicture.getId());

            // 转换为VO并返回
            PictureVO pictureVO = PictureVO.objToVo(nextPicture);

            // 增强：添加创建用户信息
            pictureVO.setUser(userService.getUserById(nextPicture.getCreateUser()));

            // 增强：添加分类信息
            try {
                List<Long> categoryIds = categoryRelationService.getCategoryIdsByContent("picture", nextPicture.getId());
                if (CollUtil.isNotEmpty(categoryIds)) {
                    Long categoryId = categoryIds.get(0);
                    pictureVO.setCategoryId(String.valueOf(categoryId));
                    String categoryName = categoryMapper.selectById(categoryId).getName();
                    pictureVO.setCategory(categoryName);
                }
            } catch (Exception e) {
                log.warn("获取下一张图片分类信息失败: {}", e.getMessage());
            }

            // 增强：添加标签信息
            try {
                List<Long> tagIds = tagRelationService.getTagIdsByContent("picture", nextPicture.getId());
                if (CollUtil.isNotEmpty(tagIds)) {
                    List<String> tagIdStrings = tagIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList());
                    pictureVO.setTagIds(tagIdStrings);

                    List<String> tagNames = tagMapper.selectNamesByIds(tagIds);
                    pictureVO.setTags(tagNames);

                    // 获取所有标签的颜色列表
                    List<Map<String, Object>> tagColorMaps = tagMapper.selectColorsByIds(tagIds);
                    if (CollUtil.isNotEmpty(tagColorMaps)) {
                        List<String> tagColors = tagColorMaps.stream()
                                .map(map -> (String) map.get("color"))
                                .collect(Collectors.toList());
                        pictureVO.setTagColors(tagColors);
                        log.debug("设置图片标签颜色: 图片ID={}, 标签数量={}, 颜色数量={}",
                                nextPicture.getId(), tagIds.size(), tagColors.size());
                    }
                }
            } catch (Exception e) {
                log.warn("获取下一张图片标签信息失败: {}", e.getMessage());
            }

            if (ObjectUtil.isNotNull(loginUser)) {
                try {
                    userReactionService.fillPictureUserReactionStatus(pictureVO, loginUser.getId());
                } catch (Exception e) {
                    log.warn("填充图片互动信息失败: {}", e.getMessage());
                }
            }

            log.info("获取下一张图片成功: 当前图片ID={}, 下一张图片ID={}", currentPictureId, nextPicture.getId());
            return pictureVO;
        } catch (Exception e) {
            log.error("获取下一张图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取下一张图片失败: " + e.getMessage());
        }
    }

    /**
     * 增加图片浏览量（异步实现）
     *
     * @param pictureId 图片ID
     */
    private void incrementViewCount(Long pictureId) {
        try {
            // 1. 立即更新Redis缓存，确保用户能立即看到最新浏览量
            String viewCountKey = RedisConstants.getReactionCountKey(
                    RedisConstants.TARGET_PICTURE, pictureId, RedisConstants.REACTION_VIEW);
            redisTemplate.opsForValue().increment(viewCountKey, 1);
            redisTemplate.expire(viewCountKey, RedisConstants.COUNT_CACHE_EXPIRE_DAYS, TimeUnit.DAYS);

            // 2. 构建消息并发送到RocketMQ进行异步处理
            PictureReactionMessage message = PictureReactionMessage.builder()
                    .pictureId(pictureId)
                    .reactionType(RedisConstants.REACTION_VIEW)
                    .operationType("add")
                    .userId(UserContext.getUserId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            // 3. 发送消息到RocketMQ
            boolean sendResult = messageProducerService.sendPictureReactionMessage(message);

            // 4. 发送失败则降级为同步更新
            if (!sendResult) {
                log.warn("发送浏览量增加消息失败，降级为同步更新: pictureId={}", pictureId);
                pictureMapper.incrementViewCount(pictureId);
            }
        } catch (Exception e) {
            log.error("增加图片浏览量失败: pictureId={}, error={}", pictureId, e.getMessage(), e);
            // 出现异常时，仍然尝试同步更新，确保基本功能可用
            try {
                pictureMapper.incrementViewCount(pictureId);
            } catch (Exception ex) {
                log.error("同步增加图片浏览量也失败: pictureId={}", pictureId, ex);
            }
        }
    }


    /**
     * 批量抓取图片
     *
     * @param requestParam 批量抓取参数
     * @param loginUser    登录用户
     * @return 上传成功的图片数量
     */
    @Override
    public int uploadPictureByBatch(PictureUploadByBatchRequest requestParam, User loginUser) {
        // 基础校验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(requestParam == null, ErrorCode.PARAMS_ERROR);

        String searchText = requestParam.getSearchText();
        ThrowUtils.throwIf(StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR, "搜索词不能为空");

        Integer count = requestParam.getCount();
        if (count == null || count <= 0) {
            count = 10;
        }
        // 限制最大抓取数量，防止滥用
        if (count > 50) {
            count = 50;
        }

        String source = requestParam.getSource();
        if (StrUtil.isBlank(source)) {
            // 默认随机选择抓取源
            source = "all";
        }

        String namePrefix = requestParam.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            // 默认使用搜索词作为前缀
            namePrefix = searchText;
        }

        // 获取分类ID
        Long categoryId = requestParam.getCategoryId();

        try {
            // 获取抓取器
            PictureCrawler crawler = pictureCrawlerManager.getCrawler(source);
            log.info("使用 {} 抓取源搜索 '{}', 数量: {}", crawler.getSourceName(), searchText, count);

            // 查询数据库中已存在的URL哈希列表
            Set<String> existingUrlHashes;
            try {
                existingUrlHashes = pictureHashMapper.getExistingUrlHashes(searchText, crawler.getSourceName());
                log.info("该搜索词 '{}' 在 {} 源已存在 {} 张图片", searchText, crawler.getSourceName(), existingUrlHashes.size());
            } catch (Exception e) {
                log.error("查询已存在的URL哈希失败", e);
                existingUrlHashes = new HashSet<>();
            }

            // 实际需要抓取的数量可能需要大于请求数量，因为会过滤重复项
            // 增加一个倍数，确保即使过滤掉重复图片，仍然有足够的图片
            int crawlCount = count * 3;

            // 执行抓取，获取可能的图片列表
            List<Map<String, Object>> pictureList = crawler.crawlPictures(searchText, crawlCount);
            log.info("成功抓取 {} 张图片，开始过滤重复图片", pictureList.size());

            // 如果未抓取到图片，返回0
            if (pictureList.isEmpty()) {
                log.warn("未抓取到任何图片");
                return 0;
            }

            // 当前会话中已处理的URL哈希集合（防止同一批次内重复）
            Set<String> sessionProcessedHashes = new HashSet<>(existingUrlHashes);

            // 过滤掉已经存在的图片URL
            List<Map<String, Object>> filteredList = new ArrayList<>();
            for (Map<String, Object> pictureData : pictureList) {
                String imageUrl = (String) pictureData.get("url");
                if (StrUtil.isBlank(imageUrl)) {
                    continue;
                }

                // 计算URL的MD5哈希值
                String urlHash = DigestUtils.md5Hex(imageUrl);

                // 如果该哈希值不在已存在的列表中，则添加到过滤后的列表
                if (!sessionProcessedHashes.contains(urlHash)) {
                    filteredList.add(pictureData);
                    // 将这个哈希值加入到已处理集合中，防止在本次批量上传中重复使用
                    sessionProcessedHashes.add(urlHash);
                } else {
                    log.debug("跳过已存在的图片URL: {}", imageUrl);
                }
            }

            log.info("过滤后剩余 {} 张非重复图片", filteredList.size());

            // 成功上传的图片数量
            int successCount = 0;

            // 成功关联到分类的图片数量
            int categoryRelatedCount = 0;

            // 如果过滤后的图片数量不足请求数量，尝试再次抓取更多图片
            if (filteredList.size() < count) {
                log.info("过滤后的图片数量不足，尝试抓取更多...");

                // 最多尝试两次额外抓取，以获取足够的图片
                for (int attempt = 0; attempt < 2 && filteredList.size() < count; attempt++) {
                    log.info("第 {} 次尝试抓取更多图片...", attempt + 1);

                    // 再次抓取更多图片，每次增加原请求数的两倍
                    List<Map<String, Object>> morePictures = crawler.crawlPictures(searchText, count * (attempt + 2));

                    // 过滤新抓取的图片
                    for (Map<String, Object> pictureData : morePictures) {
                        String imageUrl = (String) pictureData.get("url");
                        if (StrUtil.isBlank(imageUrl)) {
                            continue;
                        }

                        // 计算URL的MD5哈希值
                        String urlHash = DigestUtils.md5Hex(imageUrl);

                        // 检查是否已存在
                        if (!sessionProcessedHashes.contains(urlHash)) {
                            filteredList.add(pictureData);
                            sessionProcessedHashes.add(urlHash);

                            // 如果已经获取到足够的图片，跳出循环
                            if (filteredList.size() >= count) {
                                break;
                            }
                        }
                    }

                    log.info("额外抓取后，非重复图片总数: {}", filteredList.size());

                    // 如果已经获取到足够的图片，跳出循环
                    if (filteredList.size() >= count) {
                        break;
                    }

                    // 每次尝试之间稍微等待一下，避免频繁请求
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // 限制上传数量，确保不超过请求的数量
            int uploadCount = Math.min(filteredList.size(), count);

            // 准备批量保存图片哈希信息的列表
            List<PictureHash> pictureHashList = new ArrayList<>();

            // 批量上传图片
            for (int i = 0; i < uploadCount; i++) {
                Map<String, Object> pictureData = filteredList.get(i);
                String imageUrl = (String) pictureData.get("url");

                try {
                    // 构建上传请求参数
                    PictureUploadRequest uploadRequest = new PictureUploadRequest();
                    // 设置图片名称: 前缀 + 序号
                    uploadRequest.setPicName(namePrefix + " " + (i + 1));

                    // 调用URL上传方法
                    PictureVO pictureVO = uploadPicture(imageUrl, uploadRequest, loginUser);

                    if (pictureVO != null && pictureVO.getId() != null) {
                        try {
                            // 将上传成功的图片记录到哈希表中
                            String urlHash = DigestUtils.md5Hex(imageUrl);
                            PictureHash pictureHash = PictureHash.builder()
                                    .pictureId(pictureVO.getId())
                                    .urlHash(urlHash)
                                    .url(imageUrl)
                                    .searchText(searchText)
                                    .source(crawler.getSourceName())
                                    .createTime(LocalDateTime.now())
                                    .createUser(loginUser.getId())
                                    .isDeleted(0)
                                    .build();
                            pictureHashList.add(pictureHash);
                        } catch (Exception e) {
                            log.error("创建图片哈希对象失败: {}", e.getMessage());
                        }

                        // 将图片关联到该分类
                        if (categoryId != null && categoryId > 0) {
                            try {
                                // 创建图片与分类的关联关系
                                boolean relationResult = categoryRelationService.createCategoryRelation(
                                        categoryId, "picture", pictureVO.getId());

                                if (relationResult) {
                                    log.info("成功将图片 {} 关联到分类 {}", pictureVO.getId(), categoryId);
                                    categoryRelatedCount++;
                                } else {
                                    log.warn("将图片 {} 关联到分类 {} 失败", pictureVO.getId(), categoryId);
                                }
                            } catch (Exception e) {
                                log.error("创建图片分类关联关系时出错: {}", e.getMessage(), e);
                            }
                        }
                        successCount++;
                        log.info("成功上传第 {} 张图片: {}", i + 1, pictureVO.getId());
                    }
                } catch (Exception e) {
                    log.error("上传第 {} 张图片失败: {}", i + 1, e.getMessage());
                }

                // 每次上传后稍微等待一下，避免频繁请求
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 批量保存图片哈希信息
            if (!pictureHashList.isEmpty()) {
                try {
                    int insertCount = pictureHashMapper.batchInsert(pictureHashList);
                    log.info("成功保存 {} 条图片哈希信息，实际插入 {} 条", pictureHashList.size(), insertCount);
                } catch (Exception e) {
                    log.error("批量保存图片哈希信息失败: {}", e.getMessage(), e);
                    // 尝试单条插入
                    for (PictureHash hash : pictureHashList) {
                        try {
                            pictureHashMapper.insert(hash);
                        } catch (Exception ex) {
                            log.error("单条保存图片哈希信息失败, pictureId={}, url={}", hash.getPictureId(), hash.getUrl());
                        }
                    }
                }
            }

            // 在所有图片上传完成后，更新一次分类的内容计数
            if (categoryId != null && categoryId > 0 && categoryRelatedCount > 0) {
                try {
                    categoryService.updateCategoryContentCount(categoryId, categoryRelatedCount);
                    log.info("成功更新分类 {} 的内容计数，增加 {} 个内容", categoryId, categoryRelatedCount);
                } catch (Exception e) {
                    log.error("更新分类内容计数时出错: {}", e.getMessage(), e);
                }
            }

            log.info("批量抓取上传完成，成功: {}/{}, 过滤掉重复图片: {}",
                    successCount, uploadCount, pictureList.size() - filteredList.size());
            // 批量上传完成后清除所有缓存
            pictureCacheManager.invalidateAllCaches();
            return successCount;
        } catch (Exception e) {
            log.error("批量抓取图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量抓取图片失败: " + e.getMessage());
        }
    }


    /**
     * 转换并丰富图片列表
     * 为每个图片添加标签、分类等信息
     */
    private List<PictureVO> convertAndEnrichPictureList(List<Picture> pictureList) {
        // 转换所有图片并收集到列表中
        List<PictureVO> pictureVOList = pictureList.stream().map(picture -> {
            // 基本转换
            PictureVO pictureVO = PictureVO.objToVo(picture);
            try {
                // 设置图片创建的用户信息
                pictureVO.setUser(userService.getUserById(picture.getCreateUser()));

                // 获取并设置图片关联的分类信息 - 一张图片只关联一个分类
                List<Long> categoryIds = categoryRelationService.getCategoryIdsByContent("picture", picture.getId());
                if (CollUtil.isNotEmpty(categoryIds)) {
                    // 获取第一个分类ID
                    Long categoryId = categoryIds.get(0);
                    // 设置分类ID
                    pictureVO.setCategoryId(String.valueOf(categoryId));
                    Category category = categoryMapper.selectById(categoryId);
                    if (ObjectUtil.isNotNull(category)) {
                        // 设置分类名称
                        pictureVO.setCategory(category.getName());
                        log.debug("图片分类信息: 图片ID={}, 分类ID={}, 分类名称={}, 分类图标={}", picture.getId(), categoryId, category.getName());
                    }
                }

                // 获取并设置图片关联的标签信息 - 一张图片可以关联多个标签
                List<Long> tagIds = tagRelationService.getTagIdsByContent("picture", picture.getId());
                if (CollUtil.isNotEmpty(tagIds)) {
                    // 将所有标签ID转换为字符串列表
                    List<String> tagIdStrings = tagIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList());
                    pictureVO.setTagIds(tagIdStrings);

                    // 获取标签名称列表
                    List<String> tagNames = tagMapper.selectNamesByIds(tagIds);
                    pictureVO.setTags(tagNames);

                    // 获取所有标签的颜色列表
                    List<Map<String, Object>> tagColorMaps = tagMapper.selectColorsByIds(tagIds);
                    if (CollUtil.isNotEmpty(tagColorMaps)) {
                        List<String> tagColors = tagColorMaps.stream()
                                .map(map -> (String) map.get("color"))
                                .collect(Collectors.toList());
                        pictureVO.setTagColors(tagColors);
                        log.debug("设置图片标签颜色: 图片ID={}, 标签数量={}, 颜色数量={}",
                                picture.getId(), tagIds.size(), tagColors.size());
                    }

                    log.debug("图片标签信息: 图片ID={}, 标签数={}, 标签IDs={}", picture.getId(), tagIds.size(), String.join(",", tagIdStrings));
                }
            } catch (Exception e) {
                log.error("填充图片附加信息失败: pictureId={}", picture.getId(), e);
            }
            return pictureVO;
        }).collect(Collectors.toList());
        return pictureVOList;
    }


    /**
     * 获取首页瀑布流图片列表
     *
     * @param requestParam 请求参数（排序方式、筛选条件等）
     * @param loginUser    当前登录用户
     * @return 瀑布流图片列表包装对象
     */
    @Override
    public PictureWaterfallVO getWaterfallPictures(PictureWaterfallRequest requestParam, User loginUser) {
        // 参数规范化处理
        PictureWaterfallRequest normalizedRequest = normalizeRequest(requestParam);

        try {
            String reviewStatus = String.valueOf(PictureReviewStatusEnum.PASS.getValue());
            String cacheKey = RedisKeyConstants.buildWaterfallKey(
                    "initial",
                    normalizedRequest.getSortBy(),
                    normalizedRequest.getPageSize(),
                    normalizedRequest.getCategoryId(),
                    normalizedRequest.getTagIds(),
                    normalizedRequest.getFormat(),
                    normalizedRequest.getMinWidth(),
                    normalizedRequest.getMinHeight(),
                    normalizedRequest.getUserId(),
                    normalizedRequest.getKeyword()
            );

            // 尝试从缓存获取结果
            PictureWaterfallVO cachedResult = getCachedResult(cacheKey);
            if (cachedResult != null) {
                // 只需要填充用户交互状态，其他数据从缓存获取
                if (ObjectUtil.isNotNull(loginUser)) {
                    fillUserReactions(cachedResult.getRecords(), loginUser.getId());
                }
                return cachedResult;
            }

            // 缓存未命中，执行数据库查询
            long startTime = System.currentTimeMillis();

            // 查询图片列表
            List<Picture> pictureList = pictureMapper.selectWaterfallPictures(
                    reviewStatus,
                    normalizedRequest.getFormat(),
                    normalizedRequest.getMinWidth(),
                    normalizedRequest.getMinHeight(),
                    normalizedRequest.getUserId(),
                    normalizedRequest.getCategoryId(),
                    normalizedRequest.getTagIds(),
                    normalizedRequest.getKeyword(),
                    normalizedRequest.getSortBy(),
                    normalizedRequest.getPageSize()
            );

            // 获取总数（使用单独方法，可缓存结果）
            Long total = getWaterfallPicturesTotal(
                    reviewStatus,
                    normalizedRequest.getFormat(),
                    normalizedRequest.getMinWidth(),
                    normalizedRequest.getMinHeight(),
                    normalizedRequest.getUserId(),
                    normalizedRequest.getCategoryId(),
                    normalizedRequest.getTagIds(),
                    normalizedRequest.getKeyword()
            );

            // 构建结果对象
            PictureWaterfallVO result = buildWaterfallResult(pictureList, total, normalizedRequest.getPageSize(), normalizedRequest.getSortBy());

            // 填充用户交互状态
            if (ObjectUtil.isNotNull(loginUser)) {
                fillUserReactions(result.getRecords(), loginUser.getId());
            }

            // 将结果缓存（不包含用户交互状态）
            cacheWaterfallResult(cacheKey, result, RedisKeyConstants.WATERFALL_CACHE_EXPIRE_MINUTES);

            long endTime = System.currentTimeMillis();
            log.info("获取瀑布流图片成功: 返回{}条记录, 排序方式={}, 总数={}, 耗时={}ms",
                    result.getRecords().size(), normalizedRequest.getSortBy(), total, (endTime - startTime));

            return result;
        } catch (Exception e) {
            log.error("获取瀑布流图片列表失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取瀑布流图片列表失败: " + e.getMessage());
        }
    }

    /**
     * 加载更多瀑布流图片
     *
     * @param lastId       最后一张图片ID（游标）
     * @param lastValue    最后一张图片的排序值
     * @param requestParam 请求参数
     * @param loginUser    当前登录用户
     * @return 更多的瀑布流图片列表包装对象
     */
    @Override
    public PictureWaterfallVO loadMoreWaterfallPictures(Long lastId, Object lastValue,
                                                        PictureWaterfallRequest requestParam, User loginUser) {
        // 参数校验
        if (ObjectUtil.isNull(lastId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "lastId不能为空");
        }

        // 参数规范化处理
        PictureWaterfallRequest normalizedRequest = normalizeRequest(requestParam);

        try {
            String reviewStatus = String.valueOf(PictureReviewStatusEnum.PASS.getValue());
            String cacheKey = RedisKeyConstants.buildWaterfallKey(
                    "more-" + lastId,
                    normalizedRequest.getSortBy(),
                    normalizedRequest.getPageSize(),
                    normalizedRequest.getCategoryId(),
                    normalizedRequest.getTagIds(),
                    normalizedRequest.getFormat(),
                    normalizedRequest.getMinWidth(),
                    normalizedRequest.getMinHeight(),
                    normalizedRequest.getUserId(),
                    normalizedRequest.getKeyword()
            );

            // 尝试从缓存获取结果
            PictureWaterfallVO cachedResult = getCachedResult(cacheKey);
            if (cachedResult != null) {
                // 只需要填充用户交互状态，其他数据从缓存获取
                if (ObjectUtil.isNotNull(loginUser)) {
                    fillUserReactions(cachedResult.getRecords(), loginUser.getId());
                }
                return cachedResult;
            }

            // 缓存未命中，执行数据库查询
            long startTime = System.currentTimeMillis();

            // 查询更多图片列表
            List<Picture> pictureList = pictureMapper.selectMoreWaterfallPictures(
                    lastId,
                    lastValue,
                    reviewStatus,
                    normalizedRequest.getFormat(),
                    normalizedRequest.getMinWidth(),
                    normalizedRequest.getMinHeight(),
                    normalizedRequest.getUserId(),
                    normalizedRequest.getCategoryId(),
                    normalizedRequest.getTagIds(),
                    normalizedRequest.getKeyword(),
                    normalizedRequest.getSortBy(),
                    normalizedRequest.getPageSize()
            );

            // 获取总数（优化：可以复用前一次的总数）
            Long total = getWaterfallPicturesTotal(
                    reviewStatus,
                    normalizedRequest.getFormat(),
                    normalizedRequest.getMinWidth(),
                    normalizedRequest.getMinHeight(),
                    normalizedRequest.getUserId(),
                    normalizedRequest.getCategoryId(),
                    normalizedRequest.getTagIds(),
                    normalizedRequest.getKeyword()
            );

            // 构建结果对象
            PictureWaterfallVO result = buildWaterfallResult(pictureList, total, normalizedRequest.getPageSize(), normalizedRequest.getSortBy());

            // 填充用户交互状态
            if (ObjectUtil.isNotNull(loginUser)) {
                fillUserReactions(result.getRecords(), loginUser.getId());
            }

            // 将结果缓存（较短时间）
            cacheWaterfallResult(cacheKey, result, RedisKeyConstants.WATERFALL_MORE_CACHE_EXPIRE_MINUTES);

            long endTime = System.currentTimeMillis();
            log.info("加载更多瀑布流图片成功: 返回{}条记录, 排序方式={}, 耗时={}ms",
                    result.getRecords().size(), normalizedRequest.getSortBy(), (endTime - startTime));

            return result;
        } catch (Exception e) {
            log.error("加载更多瀑布流图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加载更多瀑布流图片失败: " + e.getMessage());
        }
    }

    /**
     * 规范化请求参数
     */
    private PictureWaterfallRequest normalizeRequest(PictureWaterfallRequest requestParam) {
        if (ObjectUtil.isNull(requestParam)) {
            requestParam = new PictureWaterfallRequest();
        }

        Integer pageSize = requestParam.getPageSize();
        if (ObjectUtil.isNull(pageSize) || pageSize <= 0 || pageSize > 100) {
            // 默认每页30条
            requestParam.setPageSize(30);
        }

        String sortBy = requestParam.getSortBy();
        if (StrUtil.isBlank(sortBy)) {
            // 默认按最新发布排序
            requestParam.setSortBy("newest");
        }

        return requestParam;
    }

    /**
     * 从缓存获取结果
     */
    private PictureWaterfallVO getCachedResult(String cacheKey) {
        try {
            Object cachedObject = redisTemplate.opsForValue().get(cacheKey);
            if (cachedObject != null) {
                log.debug("从缓存获取瀑布流数据: {}", cacheKey);
                return (PictureWaterfallVO) cachedObject;
            }
        } catch (Exception e) {
            log.warn("从缓存获取瀑布流数据失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 缓存瀑布流结果
     */
    private void cacheWaterfallResult(String cacheKey, PictureWaterfallVO result, int expireMinutes) {
        try {
            redisTemplate.opsForValue().set(cacheKey, result, expireMinutes, TimeUnit.MINUTES);
            log.debug("瀑布流数据已缓存: {}, 过期时间: {}分钟", cacheKey, expireMinutes);
        } catch (Exception e) {
            log.warn("缓存瀑布流数据失败: {}", e.getMessage());
        }
    }

    /**
     * 获取瀑布流图片总数（带缓存）
     */
    private Long getWaterfallPicturesTotal(String reviewStatus, String format, Integer minWidth,
                                           Integer minHeight, Long userId, Long categoryId,
                                           List<Long> tagIds, String keyword) {
        // 构建缓存键
        String cacheKey = RedisKeyConstants.buildCountKey(
                format, minWidth, minHeight, userId, categoryId, tagIds, keyword
        );

        try {
            // 尝试从缓存获取总数
            Object cachedCount = redisTemplate.opsForValue().get(cacheKey);
            if (cachedCount != null) {
                return (Long) cachedCount;
            }
        } catch (Exception e) {
            log.warn("从缓存获取图片总数失败: {}", e.getMessage());
        }

        // 缓存未命中，查询数据库
        Long total = pictureMapper.countWaterfallPictures(
                reviewStatus, format, minWidth, minHeight, userId, categoryId, tagIds, keyword
        );

        try {
            // 将总数缓存5分钟，避免频繁查询数据库
            redisTemplate.opsForValue().set(cacheKey, total, RedisKeyConstants.COUNT_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("缓存图片总数失败: {}", e.getMessage());
        }

        return total;
    }

    /**
     * 构建瀑布流结果对象
     */
    private PictureWaterfallVO buildWaterfallResult(List<Picture> pictureList, Long total,
                                                    Integer pageSize, String sortBy) {
        // 使用MapStruct进行基础转换
        List<PictureVO> pictureVOList = new ArrayList<>();
        if (CollUtil.isNotEmpty(pictureList)) {
            // 先进行基本转换
            pictureVOList = PictureConvert.INSTANCE.toPictureVOList(pictureList);
            // 再进行数据丰富
            enrichPictureVOList(pictureList, pictureVOList);
        }

        // 构建返回对象
        Long lastId = null;
        Object lastValue = null;
        boolean hasMore = false;

        if (CollUtil.isNotEmpty(pictureVOList)) {
            // 获取最后一张图片的ID和排序值作为下一页的游标
            PictureVO lastPicture = pictureVOList.get(pictureVOList.size() - 1);
            lastId = lastPicture.getId();

            // 根据排序方式获取对应的排序值
            switch (sortBy) {
                case "newest":
                    lastValue = lastPicture.getCreateTime();
                    break;
                case "popular":
                case "mostViewed":
                    lastValue = lastPicture.getViewCount();
                    break;
                case "mostLiked":
                    lastValue = lastPicture.getLikeCount();
                    break;
                case "mostCollected":
                    lastValue = lastPicture.getCollectionCount();
                    break;
                default:
                    lastValue = lastPicture.getCreateTime();
            }

            // 判断是否还有更多数据
            hasMore = pictureVOList.size() >= pageSize;

            // 如果提供了total，则更精确地判断是否还有更多
            if (total != null) {
                hasMore = hasMore && pictureVOList.size() < total;
            }
        }

        return PictureWaterfallVO.builder()
                .records(pictureVOList)
                .hasMore(hasMore)
                .lastId(lastId)
                .lastValue(lastValue)
                .total(total)
                .build();
    }

    /**
     * 填充用户交互状态（点赞、收藏等）
     */
    private void fillUserReactions(List<PictureVO> pictureVOList, Long userId) {
        if (CollUtil.isEmpty(pictureVOList) || userId == null) {
            return;
        }

        try {
            userReactionService.batchFillPictureUserReactionStatus(pictureVOList, userId);
        } catch (Exception e) {
            log.warn("批量填充图片互动信息失败: {}", e.getMessage());
        }
    }

    /**
     * 使用MapStruct后的数据丰富方法
     */
    private void enrichPictureVOList(List<Picture> sourceList, List<PictureVO> targetList) {
        if (CollUtil.isEmpty(sourceList) || CollUtil.isEmpty(targetList) || sourceList.size() != targetList.size()) {
            return;
        }

        // 1. 收集所有需要查询的ID
        List<Long> pictureIds = sourceList.stream()
                .map(Picture::getId)
                .collect(Collectors.toList());

        List<Long> userIds = sourceList.stream()
                .map(Picture::getCreateUser)
                .distinct()
                .collect(Collectors.toList());

        // 2. 批量查询图片的分类关系
        Map<Long, List<Long>> pictureCategoryMap = batchGetPictureCategoryIds(pictureIds);

        // 3. 批量查询所有相关的分类信息
        Set<Long> allCategoryIds = pictureCategoryMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        Map<Long, Category> categoryMap = batchGetCategories(allCategoryIds);

        // 4. 批量查询图片的标签关系
        Map<Long, List<Long>> pictureTagMap = batchGetPictureTagIds(pictureIds);

        // 5. 批量查询所有相关的标签信息
        Set<Long> allTagIds = pictureTagMap.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        // 获取标签名称和颜色
        Map<Long, TagInfo> tagInfoMap = batchGetTagInfo(allTagIds);

        // 6. 批量查询用户信息
        Map<Long, User> userMap = batchGetUsers(userIds);

        // 7. 填充VO对象
        for (int i = 0; i < sourceList.size(); i++) {
            Picture picture = sourceList.get(i);
            PictureVO pictureVO = targetList.get(i);
            Long pictureId = picture.getId();

            // 填充用户信息
            User user = userMap.get(picture.getCreateUser());
            UserConvert.INSTANCE.toUserVO(user);
            if (ObjectUtil.isNotNull(user)) {
                pictureVO.setUser(UserConvert.INSTANCE.toUserVO(user));
            }

            // 填充分类信息
            List<Long> categoryIds = pictureCategoryMap.getOrDefault(pictureId, Collections.emptyList());
            if (!categoryIds.isEmpty()) {
                Long categoryId = categoryIds.get(0);
                pictureVO.setCategoryId(String.valueOf(categoryId));

                Category category = categoryMap.get(categoryId);
                if (category != null) {
                    pictureVO.setCategory(category.getName());
                }
            }

            // 填充标签信息
            List<Long> tagIds = pictureTagMap.getOrDefault(pictureId, Collections.emptyList());
            if (!tagIds.isEmpty()) {
                // 设置标签ID
                List<String> tagIdStrings = tagIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                pictureVO.setTagIds(tagIdStrings);

                // 设置标签名称和颜色
                List<String> tagNames = new ArrayList<>();
                List<String> tagColors = new ArrayList<>();

                for (Long tagId : tagIds) {
                    TagInfo tagInfo = tagInfoMap.get(tagId);
                    if (tagInfo != null) {
                        if (tagInfo.getName() != null) {
                            tagNames.add(tagInfo.getName());
                        }
                        if (tagInfo.getColor() != null) {
                            tagColors.add(tagInfo.getColor());
                        }
                    }
                }

                pictureVO.setTags(tagNames);
                pictureVO.setTagColors(tagColors);
            }
        }
    }

    /**
     * 批量获取图片分类ID
     */
    private Map<Long, List<Long>> batchGetPictureCategoryIds(List<Long> pictureIds) {
        Map<Long, List<Long>> result = new HashMap<>();
        if (CollUtil.isEmpty(pictureIds)) {
            return result;
        }

        try {
            Map<String, List<Long>> contentCategoryMap =
                    categoryRelationService.batchGetCategoryIdsByContents("picture", pictureIds);

            if (contentCategoryMap != null) {
                // 转换key类型从String到Long
                for (Map.Entry<String, List<Long>> entry : contentCategoryMap.entrySet()) {
                    try {
                        Long pictureId = Long.valueOf(entry.getKey());
                        result.put(pictureId, entry.getValue());
                    } catch (NumberFormatException e) {
                        log.warn("解析图片ID失败: {}", entry.getKey());
                    }
                }
            }
        } catch (Exception e) {
            log.error("批量查询图片分类关系失败", e);

            // 降级：单个查询
            for (Long pictureId : pictureIds) {
                try {
                    List<Long> categoryIds = categoryRelationService.getCategoryIdsByContent("picture", pictureId);
                    if (categoryIds != null && !categoryIds.isEmpty()) {
                        result.put(pictureId, categoryIds);
                    }
                } catch (Exception ex) {
                    log.warn("查询图片分类关系失败: pictureId={}", pictureId);
                }
            }
        }

        return result;
    }

    /**
     * 批量获取分类信息
     */
    private Map<Long, Category> batchGetCategories(Set<Long> categoryIds) {
        Map<Long, Category> result = new HashMap<>();
        if (categoryIds == null || categoryIds.isEmpty()) {
            return result;
        }

        try {
            List<Category> categories = categoryMapper.selectBatchIds(new ArrayList<>(categoryIds));
            if (ObjectUtil.isNotNull(categories)) {
                for (Category category : categories) {
                    result.put(category.getId(), category);
                }
            }
        } catch (Exception e) {
            log.error("批量查询分类信息失败", e);

            // 降级：单个查询
            for (Long categoryId : categoryIds) {
                try {
                    Category category = categoryMapper.selectById(categoryId);
                    if (category != null) {
                        result.put(categoryId, category);
                    }
                } catch (Exception ex) {
                    log.warn("查询分类信息失败: categoryId={}", categoryId);
                }
            }
        }

        return result;
    }

    /**
     * 批量获取图片标签ID
     */
    private Map<Long, List<Long>> batchGetPictureTagIds(List<Long> pictureIds) {
        Map<Long, List<Long>> result = new HashMap<>();
        if (CollUtil.isEmpty(pictureIds)) {
            return result;
        }

        try {
            Map<String, List<Long>> contentTagMap =
                    tagRelationService.batchGetTagIdsByContents("picture", pictureIds);

            if (contentTagMap != null) {
                // 转换key类型从String到Long
                for (Map.Entry<String, List<Long>> entry : contentTagMap.entrySet()) {
                    try {
                        Long pictureId = Long.valueOf(entry.getKey());
                        result.put(pictureId, entry.getValue());
                    } catch (NumberFormatException e) {
                        log.warn("解析图片ID失败: {}", entry.getKey());
                    }
                }
            }
        } catch (Exception e) {
            log.error("批量查询图片标签关系失败", e);

            // 降级：单个查询
            for (Long pictureId : pictureIds) {
                try {
                    List<Long> tagIds = tagRelationService.getTagIdsByContent("picture", pictureId);
                    if (tagIds != null && !tagIds.isEmpty()) {
                        result.put(pictureId, tagIds);
                    }
                } catch (Exception ex) {
                    log.warn("查询图片标签关系失败: pictureId={}", pictureId);
                }
            }
        }

        return result;
    }

    /**
     * 批量获取标签信息
     */
    private Map<Long, TagInfo> batchGetTagInfo(Set<Long> tagIds) {
        Map<Long, TagInfo> result = new HashMap<>();
        if (tagIds == null || tagIds.isEmpty()) {
            return result;
        }

        try {
            // 获取标签名称和颜色
            List<Map<String, Object>> tagInfos = tagMapper.selectNamesAndColorsByIds(new ArrayList<>(tagIds));
            if (tagInfos != null) {
                for (Map<String, Object> info : tagInfos) {
                    Long tagId = (Long) info.get("id");
                    String name = (String) info.get("name");
                    String color = (String) info.get("color");

                    TagInfo tagInfo = new TagInfo();
                    tagInfo.setName(name);
                    tagInfo.setColor(color);

                    result.put(tagId, tagInfo);
                }
            }
        } catch (Exception e) {
            log.error("批量查询标签信息失败", e);
        }

        return result;
    }

    /**
     * 批量获取用户信息
     */
    private Map<Long, User> batchGetUsers(List<Long> userIds) {
        Map<Long, User> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        try {
            for (Long userId : userIds) {
                User user = userMapper.selectById(userId);
                if (Objects.nonNull(user)) {
                    result.put(userId, user);
                }
            }
        } catch (Exception e) {
            log.error("批量查询用户信息失败", e);
        }

        return result;
    }

    /**
     * 校验图片信息
     *
     * @param picture 图片信息
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(ObjectUtil.isNull(picture), ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getDescription();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }


    /**
     * 图片校验
     *
     * @param loginUser 登录用户
     * @param picture   图片信息
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        // 公共图库，仅本人或管理员可操作
        if (!picture.getCreateUser().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }


    /**
     * 编辑图片信息（包括分类和标签）
     * 修复标签引用计数重复问题
     *
     * @param requestParam 编辑参数
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean editPicture(PictureEditRequest requestParam) {
        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(requestParam) || requestParam.getId() <= 0,
                ErrorCode.PARAMS_ERROR);

        Long pictureId = requestParam.getId();

        // 获取当前登录用户
        User loginUser = UserContext.getUser();
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser), ErrorCode.NO_AUTH_ERROR);

        // 查询原图片信息，判断是否存在
        Picture oldPicture = pictureMapper.selectById(pictureId);
        ThrowUtils.throwIf(ObjectUtil.isNull(oldPicture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 校验权限（仅本人或管理员可编辑）
        checkPictureAuth(loginUser, oldPicture);

        // 1. 更新图片基本信息
        Picture picture = new Picture();
        picture.setId(pictureId);
        picture.setName(requestParam.getName());
        picture.setDescription(requestParam.getDescription());
        picture.setUpdateTime(LocalDateTime.now());
        picture.setUpdateUser(loginUser.getId());

        // 填充审核参数（非管理员编辑时需要重新审核）
        fillReviewParams(picture, loginUser);

        // 数据校验
        validPicture(picture);

        // 更新图片基本信息
        boolean updateResult = pictureMapper.updateByPrimaryKeySelective(picture) > 0;
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新图片信息失败");

        try {
            List<Long> oldCategoryIds = null;
            // 2. 更新分类关联
            if (requestParam.getCategoryId() != null && requestParam.getCategoryId() > 0) {
                // 获取原分类关联
                oldCategoryIds = categoryRelationService.getCategoryIdsByContent("picture", pictureId);
                // 准备新分类ID列表
                List<Long> newCategoryIds = List.of(requestParam.getCategoryId().longValue());

                // 更新分类关联关系
                categoryRelationService.updateContentCategories(newCategoryIds, "picture", pictureId);

                // 更新分类内容计数
                if (CollUtil.isNotEmpty(oldCategoryIds)) {
                    // 如果原来有分类，减少原分类的内容计数
                    for (Long oldCategoryId : oldCategoryIds) {
                        if (!newCategoryIds.contains(oldCategoryId)) {
                            categoryService.updateCategoryContentCount(oldCategoryId, -1);
                            log.info("减少原分类 {} 的内容计数", oldCategoryId);
                        }
                    }
                }

                // 如果新分类不在原分类列表中，增加新分类的内容计数
                if (CollUtil.isEmpty(oldCategoryIds) || !oldCategoryIds.contains(requestParam.getCategoryId().longValue())) {
                    categoryService.updateCategoryContentCount(requestParam.getCategoryId().longValue(), 1);
                    log.info("增加新分类 {} 的内容计数", requestParam.getCategoryId());
                }
            }

            // 3. 更新标签关联
            // 3.1 获取原标签关联
            List<Long> oldTagIds = tagRelationService.getTagIdsByContent("picture", pictureId);

            // 3.2 处理新标签列表
            List<Long> newTagIds = new ArrayList<>();
            if (CollUtil.isNotEmpty(requestParam.getTagIds())) {
                newTagIds.addAll(requestParam.getTagIds());
            }

            // 3.3 只有在标签发生变化时才处理
            if (!CollUtil.isEqualList(oldTagIds, newTagIds)) {
                log.info("图片标签发生变化，准备更新标签关联和引用计数");

                // 3.4 清除所有旧的标签关联
                if (CollUtil.isNotEmpty(oldTagIds)) {
                    tagRelationService.deleteAllTagRelations("picture", pictureId);
                    log.info("已清除图片 {} 的所有标签关联", pictureId);
                }

                // 3.5 创建所有新的标签关联
                for (Long tagId : newTagIds) {
                    boolean result = tagRelationService.createTagRelation(tagId, "picture", pictureId);
                    log.info("创建标签关联: 标签={}, 图片ID={}, 结果={}", tagId, pictureId, result ? "成功" : "失败");
                }

                // 3.6 更新标签引用计数
                // 减少不再使用的标签的引用计数
                if (CollUtil.isNotEmpty(oldTagIds)) {
                    for (Long oldTagId : oldTagIds) {
                        if (!newTagIds.contains(oldTagId)) {
                            tagService.updateTagReferenceCount(oldTagId, -1);
                            log.info("减少标签 {} 的引用计数", oldTagId);
                        }
                    }
                }

                // 增加新使用的标签的引用计数
                for (Long newTagId : newTagIds) {
                    if (CollUtil.isEmpty(oldTagIds) || !oldTagIds.contains(newTagId)) {
                        tagService.updateTagReferenceCount(newTagId, 1);
                        log.info("增加标签 {} 的引用计数", newTagId);
                    }
                }

                log.info("标签关联更新完成：原标签数={}, 新标签数={}, 图片ID={}",
                        oldTagIds != null ? oldTagIds.size() : 0,
                        newTagIds.size(),
                        pictureId);
            } else {
                log.info("图片标签未发生变化，无需更新");
            }

            log.info("更新图片成功: pictureId={}, categoryId={}, tagIds={}",
                    pictureId, requestParam.getCategoryId(), requestParam.getTagIds());
            // 清除缓存
            pictureCacheManager.invalidateAfterPictureEdit(
                    pictureId,
                    CollUtil.isNotEmpty(oldCategoryIds) ? oldCategoryIds.get(0) : null,
                    requestParam.getCategoryId() != null ? requestParam.getCategoryId().longValue() : null,
                    oldTagIds,
                    newTagIds
            );
            log.info("已清除图片编辑后的相关缓存: pictureId={}", pictureId);
            return true;
        } catch (Exception e) {
            log.error("更新图片关联关系失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图片关联关系失败: " + e.getMessage());
        }
    }


    /**
     * 查找相似图片
     *
     * @param request   查询参数
     * @param loginUser 当前登录用户
     * @return 分页结果
     */
    @Override
    public PageResult<PictureVO> findSimilarPictures(SimilarPictureRequest request, User loginUser) {
        if (request == null || request.getPictureId() == null || request.getPictureId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        }

        // 查询原图片信息
        final Long pictureId = request.getPictureId();
        Picture originalPicture = pictureMapper.selectById(pictureId);
        if (originalPicture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "原图片不存在");
        }

        // 获取原图片分类
        List<Long> originalCategoryIds = categoryRelationService.getCategoryIdsByContent("picture", pictureId);
        if (CollUtil.isEmpty(originalCategoryIds)) {
            log.warn("图片 {} 未关联任何分类", pictureId);
            // 如果原图片没有分类，返回空结果
            return PageResult.empty(request.getPageNum(), request.getPageSize());
        }

        // 获取原图片的分类ID
        final Long originalCategoryId = originalCategoryIds.get(0);

        // 获取该分类的相关分类（父分类、子分类和同级分类）
        Set<Long> relatedCategoryIds = getRelatedCategoryIds(originalCategoryId);
        if (relatedCategoryIds.isEmpty()) {
            relatedCategoryIds.add(originalCategoryId);
        }

        log.info("图片 {} 的相关分类IDs: {}", pictureId, relatedCategoryIds);

        // 获取原图片标签
        final List<Long> tagIds = tagRelationService.getTagIdsByContent("picture", pictureId);

        // 宽高比
        final Double picScale = originalPicture.getPicScale() != null
                ? originalPicture.getPicScale().doubleValue()
                : null;

        // 主色调
        final String mainColor = originalPicture.getMainColor();

        // 是否排除同一用户
        final Long finalExcludeUserId;
        if (request.getIncludeSameUser() != null && !request.getIncludeSameUser()) {
            finalExcludeUserId = originalPicture.getCreateUser();
        } else {
            finalExcludeUserId = null;
        }

        // 使用PageHelper进行分页查询，注意这里不再传入minSimilarity参数
        return PageUtils.doPage(request, () ->
                        pictureMapper.selectSimilarPictures(
                                pictureId,
                                originalCategoryId,
                                tagIds,
                                picScale,
                                mainColor,
                                finalExcludeUserId,
                                request.getMatchType(),
                                new ArrayList<>(relatedCategoryIds)
                        ),
                this::convertAndEnrichPicture
        );
    }


    /**
     * 获取指定分类的相关分类ID（包括父分类、子分类和同级分类）
     *
     * @param categoryId 分类ID
     * @return 相关分类ID集合
     */
    private Set<Long> getRelatedCategoryIds(Long categoryId) {
        Set<Long> relatedCategoryIds = new HashSet<>();
        try {
            // 添加当前分类
            relatedCategoryIds.add(categoryId);

            // 获取当前分类信息
            Category category = categoryMapper.selectById(categoryId);
            if (category == null) {
                return relatedCategoryIds;
            }

            // 添加父分类
            if (category.getParentId() != null && category.getParentId() > 0) {
                relatedCategoryIds.add(category.getParentId());

                // 获取同级分类（兄弟分类）
                List<Category> siblingCategories = categoryMapper.selectByParentId(category.getParentId());
                if (CollUtil.isNotEmpty(siblingCategories)) {
                    for (Category sibling : siblingCategories) {
                        relatedCategoryIds.add(sibling.getId());
                    }
                }
            }

            // 获取子分类
            List<Category> childCategories = categoryMapper.selectByParentId(categoryId);
            if (CollUtil.isNotEmpty(childCategories)) {
                for (Category child : childCategories) {
                    relatedCategoryIds.add(child.getId());
                }
            }

            log.info("分类 {} 的相关分类数量: {}", categoryId, relatedCategoryIds.size());
        } catch (Exception e) {
            log.error("获取相关分类失败", e);
        }
        return relatedCategoryIds;
    }

    /**
     * 转换并丰富单个图片对象
     */
    private PictureVO convertAndEnrichPicture(Picture picture) {
        // 基本转换
        PictureVO pictureVO = PictureConvert.INSTANCE.toPictureVO(picture);

        try {
            // 设置图片创建的用户信息
            pictureVO.setUser(userService.getUserById(picture.getCreateUser()));

            // 获取并设置图片关联的分类信息
            List<Long> categoryIds = categoryRelationService.getCategoryIdsByContent("picture", picture.getId());
            if (CollUtil.isNotEmpty(categoryIds)) {
                Long categoryId = categoryIds.get(0);
                pictureVO.setCategoryId(String.valueOf(categoryId));
                Category category = categoryMapper.selectById(categoryId);
                if (category != null) {
                    pictureVO.setCategory(category.getName());
                }
            }

            // 获取并设置图片关联的标签信息
            List<Long> tagIds = tagRelationService.getTagIdsByContent("picture", picture.getId());
            if (CollUtil.isNotEmpty(tagIds)) {
                List<String> tagIdStrings = tagIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                pictureVO.setTagIds(tagIdStrings);

                List<String> tagNames = tagMapper.selectNamesByIds(tagIds);
                pictureVO.setTags(tagNames);

                // 获取标签颜色
                List<Map<String, Object>> tagColorMaps = tagMapper.selectColorsByIds(tagIds);
                if (CollUtil.isNotEmpty(tagColorMaps)) {
                    List<String> tagColors = tagColorMaps.stream()
                            .map(map -> (String) map.get("color"))
                            .collect(Collectors.toList());
                    pictureVO.setTagColors(tagColors);
                }
            }
        } catch (Exception e) {
            log.error("填充图片附加信息失败: pictureId={}", picture.getId(), e);
        }

        return pictureVO;
    }

    /**
     * 以图搜图功能
     *
     * @param request   请求参数
     * @param loginUser 当前登录用户
     * @return 分页结果
     */
    @Override
    public PageResult<PictureVO> searchByImage(ImageSearchRequest request, User loginUser) {
        if (request == null || request.getPictureId() == null || request.getPictureId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        }

        // 获取页大小 - 这个就是我们要限制的总记录数
        int pageSize = request.getPageSize();

        try {
            // 步骤1: 查询原图片信息
            Picture sourcePicture = pictureMapper.selectById(request.getPictureId());
            if (sourcePicture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "原图片不存在");
            }

            // 步骤2: 构建搜索关键词 - 优化后的逻辑
            List<String> searchKeywords = new ArrayList<>();

            // 优先级1: 使用自定义关键词
            if (StrUtil.isNotBlank(request.getCustomKeyword())) {
                searchKeywords.add(request.getCustomKeyword());
                log.info("使用自定义关键词: {}", request.getCustomKeyword());
            }
            // 优先级2: 首次搜索且图片名称不为空，使用图片名称
            else if (!request.getIsResearch() && StrUtil.isNotBlank(sourcePicture.getName())) {
                searchKeywords.add(sourcePicture.getName());
                log.info("优先使用图片名称作为关键词: {}", sourcePicture.getName());
            }
            // 优先级3: 再次搜索或图片名称为空时
            else {
                // 3a. 先尝试获取标签信息
                List<Long> tagIds = tagRelationService.getTagIdsByContent("picture", request.getPictureId());
                if (!tagIds.isEmpty()) {
                    List<String> tagNames = tagMapper.selectNamesByIds(tagIds);
                    if (!tagNames.isEmpty()) {
                        // 只取前3个标签，避免搜索过于复杂
                        searchKeywords.addAll(tagNames.subList(0, Math.min(tagNames.size(), 3)));
                        log.info("使用标签关键词: {}", String.join(", ", tagNames.subList(0, Math.min(tagNames.size(), 3))));
                    }
                }
                // 3b. 标签为空时，使用分类信息
                else {
                    // 获取分类信息
                    List<Long> categoryIds = categoryRelationService.getCategoryIdsByContent("picture", request.getPictureId());
                    if (!categoryIds.isEmpty()) {
                        Long categoryId = categoryIds.get(0);
                        Category category = categoryMapper.selectById(categoryId);
                        if (category != null) {
                            searchKeywords.add(category.getName());
                            log.info("使用分类关键词: {}", category.getName());
                        }
                    }
                }

                // 优先级4: 如果分类和标签都为空，且图片名称不为空，则仍使用图片名称
                if (searchKeywords.isEmpty() && StrUtil.isNotBlank(sourcePicture.getName())) {
                    searchKeywords.add(sourcePicture.getName());
                    log.info("回退使用图片名称作为关键词: {}", sourcePicture.getName());
                }

                // 优先级5: 如果所有信息都为空，使用默认关键词
                if (searchKeywords.isEmpty()) {
                    searchKeywords.add("dog");
                    log.info("使用默认关键词: dog");
                }
            }

            // 步骤3: 将多个关键词合并为一个搜索词，以空格分隔
            String searchText = String.join(" ", searchKeywords);
            log.info("最终生成的搜索关键词: {}", searchText);

            // 步骤4: 使用PictureCrawlerManager搜索图片
            PictureCrawler crawler = pictureCrawlerManager.getCrawler(request.getSource());

            List<Map<String, Object>> crawledPictures = crawler.crawlPictures(searchText, pageSize * 2);
            log.info("爬取到{}张图片，开始验证URL", crawledPictures.size());

            // 步骤5: 验证图片URL是否可访问
            List<Map<String, Object>> validPictures = validateImageUrls(crawledPictures);
            log.info("验证后有{}张有效图片", validPictures.size());

            // 如果有效图片超过pageSize，限制为pageSize数量
            if (validPictures.size() > pageSize) {
                validPictures = validPictures.subList(0, pageSize);
                log.info("限制结果为请求的{}条记录", pageSize);
            }

            // 步骤6: 将验证通过的搜索结果上传到腾讯云并转换为PictureVO对象
            List<PictureVO> pictureVOList = uploadAndConvertSearchResults(validPictures, sourcePicture, searchText, crawler.getSourceName(), loginUser);

            // 最终再次确保不超过pageSize
            if (pictureVOList.size() > pageSize) {
                pictureVOList = pictureVOList.subList(0, pageSize);
            }

            // 创建分页结果 - 因为总数现在等于展示数量，所以都是pageSize
            long total = pictureVOList.size();

            // 只需返回单页结果，不需要分页
            return PageResult.build(total, pictureVOList, 1, pageSize);

        } catch (Exception e) {
            log.error("以图搜图过程中发生错误", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "搜索图片失败: " + e.getMessage());
        }
    }

    /**
     * 验证图片URL是否可访问
     *
     * @param pictures 待验证的图片数据列表
     * @return 验证通过的图片数据列表
     */
    private List<Map<String, Object>> validateImageUrls(List<Map<String, Object>> pictures) {
        List<Map<String, Object>> validPictures = new ArrayList<>();

        for (Map<String, Object> picture : pictures) {
            String imageUrl = (String) picture.get("url");
            if (StrUtil.isBlank(imageUrl)) {
                continue;
            }

            try {
                // 发送HEAD请求检查URL是否可访问
                HttpResponse response = HttpUtil.createRequest(Method.HEAD, imageUrl)
                        .setConnectionTimeout(3000)
                        .setReadTimeout(3000)
                        .execute();

                if (response.getStatus() == HttpStatus.HTTP_OK) {
                    // 检查Content-Type确保是图片类型
                    String contentType = response.header("Content-Type");
                    if (StrUtil.isNotBlank(contentType) && contentType.startsWith("image/")) {
                        validPictures.add(picture);
                        log.debug("URL验证通过: {}", imageUrl);
                    } else {
                        log.warn("非图片类型资源，Content-Type: {}, URL: {}", contentType, imageUrl);
                    }
                } else {
                    log.warn("图片URL不可访问，状态码: {}, URL: {}", response.getStatus(), imageUrl);
                }
            } catch (Exception e) {
                log.warn("验证图片URL失败: {}, 错误: {}", imageUrl, e.getMessage());
            }
        }

        log.info("URL验证结果: {}张有效/{}张总数", validPictures.size(), pictures.size());
        return validPictures;
    }

    /**
     * 将搜索结果上传到腾讯云并转换为PictureVO对象
     * 修改后的方法，确保每个图片都先上传到腾讯云再返回
     */
    private List<PictureVO> uploadAndConvertSearchResults(List<Map<String, Object>> crawledPictures,
                                                          Picture sourcePicture, String searchText, String sourceName, User loginUser) {
        List<PictureVO> pictureVOList = new ArrayList<>();

        // 获取源图片的分类和标签信息（用于填充搜索结果）
        List<Long> categoryIds = categoryRelationService.getCategoryIdsByContent("picture", sourcePicture.getId());
        List<Long> tagIds = tagRelationService.getTagIdsByContent("picture", sourcePicture.getId());

        for (Map<String, Object> pictureData : crawledPictures) {
            String imageUrl = (String) pictureData.get("url");
            if (StrUtil.isBlank(imageUrl)) {
                continue;
            }

            try {
                // 将图片上传到腾讯云
                UploadPictureResult uploadResult = uploadSearchResultImage(imageUrl, sourcePicture.getId(), loginUser);

                String title = (String) pictureData.getOrDefault("title", "搜索结果");
                if (StrUtil.isBlank(title)) {
                    title = "搜索: " + searchText;
                }

                // 创建并填充PictureVO对象
                PictureVO pictureVO = new PictureVO();
                // 使用腾讯云返回的URL，而不是原始URL
                pictureVO.setUrl(uploadResult.getUrl());
                pictureVO.setThumbnailUrl(uploadResult.getThumbnailUrl() != null ?
                        uploadResult.getThumbnailUrl() : uploadResult.getUrl());
                pictureVO.setName(title);

                // 设置尺寸信息，优先使用上传结果中的尺寸信息
                pictureVO.setPicWidth(uploadResult.getPicWidth());
                pictureVO.setPicHeight(uploadResult.getPicHeight());
                pictureVO.setPicScale(uploadResult.getPicScale());

                // 设置图片格式
                pictureVO.setFormat(uploadResult.getPicFormat());

                // 设置图片大小
                pictureVO.setSize(uploadResult.getPicSize());

                // 设置主色调
                pictureVO.setMainColor(uploadResult.getPicColor());

                // 设置时间和描述
                pictureVO.setCreateTime(LocalDateTime.now());
                pictureVO.setDescription("从" + sourceName + "搜索: " + searchText);

                // 生成临时ID（负数，表示未保存到数据库）
                pictureVO.setId(-System.currentTimeMillis() - new Random().nextInt(1000));

                // 填充分类信息
                if (!categoryIds.isEmpty()) {
                    pictureVO.setCategoryId(categoryIds.get(0).toString());
                    Category category = categoryMapper.selectById(categoryIds.get(0));
                    if (category != null) {
                        pictureVO.setCategory(category.getName());
                    }
                }

                // 填充标签信息
                if (!tagIds.isEmpty()) {
                    List<String> tagNames = tagMapper.selectNamesByIds(tagIds);
                    pictureVO.setTags(tagNames);

                    List<String> tagIdStrings = tagIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList());
                    pictureVO.setTagIds(tagIdStrings);

                    // 处理标签颜色
                    List<Map<String, Object>> tagColorMaps = tagMapper.selectColorsByIds(tagIds);
                    if (CollUtil.isNotEmpty(tagColorMaps)) {
                        List<String> tagColors = tagColorMaps.stream()
                                .map(map -> (String) map.get("color"))
                                .collect(Collectors.toList());
                        pictureVO.setTagColors(tagColors);
                    }
                }

                // 设置用户信息（原始图片的上传者）
                pictureVO.setUser(userService.getUserById(sourcePicture.getCreateUser()));

                pictureVOList.add(pictureVO);
                log.info("成功将搜索结果图片上传至腾讯云: 原URL={}, 新URL={}", imageUrl, uploadResult.getUrl());
            } catch (Exception e) {
                log.error("处理搜索结果图片失败: {}", e.getMessage(), e);
                // 如果单张图片处理失败，继续处理其他图片
            }
        }

        return pictureVOList;
    }


    /**
     * 上传搜索结果图片到腾讯云（不保存到数据库）
     *
     * @param imageUrl      图片URL
     * @param sourceImageId 源图片ID（用于构建上传路径）
     * @param loginUser     当前登录用户
     * @return 上传结果
     */
    private UploadPictureResult uploadSearchResultImage(String imageUrl, Long sourceImageId, User loginUser) {
        try {
            // 校验图片URL有效性
            urlPictureUpload.validPicture(imageUrl);

            // 使用特定的路径前缀，防止和普通上传图片混淆
            String uploadPathPrefix = String.format("search-results/%s/%s", loginUser.getId(), sourceImageId);

            // 调用URL图片上传模板方法上传图片到腾讯云
            UploadPictureResult result = urlPictureUpload.uploadPicture(imageUrl, uploadPathPrefix);

            log.info("搜索结果图片上传成功: 原URL={}, 腾讯云URL={}", imageUrl, result.getUrl());
            return result;
        } catch (Exception e) {
            log.error("搜索结果图片上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 保存搜索结果关系
     *
     * @param sourceId  源图片ID
     * @param targetIds 目标图片ID列表（临时ID）
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean saveSearchResults(Long sourceId, List<Long> targetIds) {
        if (sourceId == null || CollUtil.isEmpty(targetIds)) {
            return false;
        }

        try {
            // 这里实现搜索结果关系的记录逻辑
            // 可以保存到专门的搜索历史表中，记录源图片ID和搜索结果
            log.info("保存搜索结果关系: sourceId={}, targetCount={}", sourceId, targetIds.size());
            return true;
        } catch (Exception e) {
            log.error("保存搜索结果关系失败: sourceId={}", sourceId, e);
            return false;
        }
    }


//==================== 下面是后台管理相关代码 ===============================


    /**
     * 管理员查询图片列表（分页）
     *
     * @param queryRequest 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<PictureVO> adminListPicturesByPage(AdminPictureQueryRequest queryRequest) {
        // 使用PageUtils进行分页查询，适用于复杂条件的分页
        return PageUtils.doPage(queryRequest,
                // 数据查询
                () -> pictureMapper.adminListPictures(queryRequest),
                // 数据转换，使用MapStruct进行对象映射
                picture -> {
                    // 基本转换
                    PictureVO pictureVO = PictureConvert.INSTANCE.toPictureVO(picture);

                    try {
                        // 设置图片创建的用户信息
                        pictureVO.setUser(userService.getUserById(picture.getCreateUser()));

                        // 添加审核人信息
                        if (ObjectUtil.isNotNull(picture.getReviewUserId())) {
                            // 查询审核人用户信息
                            UserVO reviewerUser = userService.getUserById(picture.getReviewUserId());
                            if (ObjectUtil.isNotNull(reviewerUser)) {
                                pictureVO.setReviewerUserName(reviewerUser.getUsername());
                            }
                        }

                        // 获取并设置图片关联的分类信息
                        List<Long> categoryIds = categoryRelationService.getCategoryIdsByContent("picture", picture.getId());
                        if (CollUtil.isNotEmpty(categoryIds)) {
                            Long categoryId = categoryIds.get(0);
                            pictureVO.setCategoryId(String.valueOf(categoryId));

                            try {
                                // 获取分类名称
                                String categoryName = categoryMapper.selectById(categoryId).getName();
                                pictureVO.setCategory(categoryName);
                            } catch (Exception e) {
                                log.warn("获取图片分类名称失败: pictureId={}, categoryId={}", picture.getId(), categoryId);
                            }
                        }

                        // 获取并设置图片关联的标签信息
                        List<Long> tagIds = tagRelationService.getTagIdsByContent("picture", picture.getId());
                        if (CollUtil.isNotEmpty(tagIds)) {
                            // 将所有标签ID转换为字符串列表
                            List<String> tagIdStrings = tagIds.stream()
                                    .map(String::valueOf)
                                    .collect(java.util.stream.Collectors.toList());
                            pictureVO.setTagIds(tagIdStrings);

                            // 获取标签名称列表
                            List<String> tagNames = tagMapper.selectNamesByIds(tagIds);
                            pictureVO.setTags(tagNames);

                            // 获取标签颜色
                            List<Map<String, Object>> tagColorMaps = tagMapper.selectColorsByIds(tagIds);
                            if (CollUtil.isNotEmpty(tagColorMaps)) {
                                List<String> tagColors = tagColorMaps.stream()
                                        .map(map -> (String) map.get("color"))
                                        .collect(Collectors.toList());
                                pictureVO.setTagColors(tagColors);
                                log.debug("设置图片标签颜色: pictureId={}, 标签数量={}, 颜色数量={}",
                                        picture.getId(), tagIds.size(), tagColors.size());
                            }
                        }
                    } catch (Exception e) {
                        log.error("填充图片附加信息失败: pictureId={}", picture.getId(), e);
                    }

                    return pictureVO;
                }
        );
    }

    /**
     * 管理员删除图片
     *
     * @param pictureId 图片ID
     * @param userId    操作用户ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean adminDeletePicture(Long pictureId, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureId) || pictureId <= 0,
                ErrorCode.PARAMS_ERROR, "图片ID不合法");

        // 查询图片是否存在
        Picture picture = pictureMapper.selectById(pictureId);
        ThrowUtils.throwIf(ObjectUtil.isNull(picture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        ThrowUtils.throwIf(picture.getIsDeleted() == 1, ErrorCode.OPERATION_ERROR, "图片已被删除");

        try {
            // 1. 处理图片与标签的关联关系
            // 先获取图片关联的所有标签ID
            List<Long> tagIds = tagRelationService.getTagIdsByContent("picture", pictureId);
            if (CollUtil.isNotEmpty(tagIds)) {
                // 遍历所有关联的标签，减少每个标签的引用计数
                for (Long tagId : tagIds) {
                    tagService.updateTagReferenceCount(tagId, -1);
                    log.info("减少标签引用计数: tagId={}, pictureId={}", tagId, pictureId);
                }

                // 删除图片的所有标签关联关系，但不再重复更新引用计数
                tagRelationService.deleteAllTagRelations("picture", pictureId);
                log.info("删除图片的所有标签关系: pictureId={}, 标签数量={}", pictureId, tagIds.size());
            }

            // 2. 处理图片与分类的关联关系
            // 获取图片关联的所有分类ID
            List<Long> categoryIds = categoryRelationService.getCategoryIdsByContent("picture", pictureId);
            if (CollUtil.isNotEmpty(categoryIds)) {
                // 减少每个分类的内容计数
                for (Long categoryId : categoryIds) {
                    categoryService.updateCategoryContentCount(categoryId, -1);
                    log.info("减少分类内容计数: categoryId={}, pictureId={}", categoryId, pictureId);
                }

                // 删除图片的所有分类关联关系
                categoryRelationService.deleteAllRelationsByContent("picture", pictureId);
                log.info("删除图片的所有分类关系: pictureId={}, 分类数量={}", pictureId, categoryIds.size());
            }

            // 3. 逻辑删除图片
            Picture updatePicture = new Picture();
            updatePicture.setId(pictureId);
            updatePicture.setIsDeleted(1);
            updatePicture.setUpdateTime(LocalDateTime.now());
            updatePicture.setUpdateUser(userId);

            int result = pictureMapper.updateByPrimaryKeySelective(updatePicture);
            ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR, "删除图片失败");

            // 4. 清除所有相关缓存
            pictureCacheManager.invalidateAfterPictureDelete(pictureId);

            // 清除主页瀑布流相关缓存
            pictureCacheManager.invalidateAllCaches();

            log.info("管理员 {} 删除图片成功: pictureId={}", userId, pictureId);
            return true;
        } catch (Exception e) {
            log.error("管理员删除图片失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除图片失败: " + e.getMessage());
        }
    }
ˆ
    /**
     * 管理员批量删除图片
     *
     * @param pictureIds 图片ID列表
     * @param userId     操作用户ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean adminBatchDeletePictures(List<Long> pictureIds, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIds), ErrorCode.PARAMS_ERROR, "图片ID列表不能为空");

        try {
            // 处理每张图片的关联关系和计数
            for (Long pictureId : pictureIds) {
                // 1. 处理图片与标签的关联关系
                List<Long> tagIds = tagRelationService.getTagIdsByContent("picture", pictureId);
                if (CollUtil.isNotEmpty(tagIds)) {
                    // 减少每个标签的引用计数
                    for (Long tagId : tagIds) {
                        tagService.updateTagReferenceCount(tagId, -1);
                        log.info("减少标签引用计数: tagId={}, pictureId={}", tagId, pictureId);
                    }

                    // 删除图片的所有标签关联关系
                    tagRelationService.deleteAllTagRelations("picture", pictureId);
                    log.info("删除图片的所有标签关系: pictureId={}, 标签数量={}", pictureId, tagIds.size());
                }

                // 2. 处理图片与分类的关联关系
                List<Long> categoryIds = categoryRelationService.getCategoryIdsByContent("picture", pictureId);
                if (CollUtil.isNotEmpty(categoryIds)) {
                    // 减少每个分类的内容计数
                    for (Long categoryId : categoryIds) {
                        categoryService.updateCategoryContentCount(categoryId, -1);
                    }

                    // 删除图片的所有分类关联关系
                    categoryRelationService.deleteAllRelationsByContent("picture", pictureId);
                }
            }

            // 3. 批量逻辑删除图片
            int result = pictureMapper.batchLogicDelete(pictureIds, userId);

            // 4. 清除缓存
            for (Long pictureId : pictureIds) {
                pictureCacheManager.invalidateAfterPictureDelete(pictureId);
            }

            // 清除所有页面缓存（批量操作影响面广）
            pictureCacheManager.invalidateAllCaches();

            log.info("管理员 {} 批量删除图片成功: 影响行数={}, pictureIds={}", userId, result, pictureIds);
            return true;
        } catch (Exception e) {
            log.error("管理员批量删除图片失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量删除图片失败: " + e.getMessage());
        }
    }

    /**
     * 管理员审核图片
     *
     * @param reviewRequest 审核请求
     * @param userId        操作用户ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean adminReviewPicture(PictureReviewRequest reviewRequest, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(reviewRequest), ErrorCode.PARAMS_ERROR, "审核参数不能为空");
        Long pictureId = reviewRequest.getPictureId();
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureId) || pictureId <= 0,
                ErrorCode.PARAMS_ERROR, "图片ID不合法");

        // 查询图片是否存在
        Picture picture = pictureMapper.selectById(pictureId);
        ThrowUtils.throwIf(ObjectUtil.isNull(picture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        ThrowUtils.throwIf(picture.getIsDeleted() == 1, ErrorCode.OPERATION_ERROR, "图片已被删除");

        try {
            // 更新图片审核状态
            Picture updatePicture = new Picture();
            updatePicture.setId(pictureId);
            updatePicture.setReviewStatus(reviewRequest.getReviewStatus());
            updatePicture.setReviewMessage(reviewRequest.getReviewMessage());
            updatePicture.setReviewUserId(userId);
            updatePicture.setReviewTime(LocalDateTime.now());
            updatePicture.setUpdateTime(LocalDateTime.now());
            updatePicture.setUpdateUser(userId);

            int result = pictureMapper.updateByPrimaryKeySelective(updatePicture);
            ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR, "审核图片失败");

            pictureCacheManager.invalidateAfterReviewStatusChange();

            log.info("管理员 {} 审核图片成功: pictureId={}, 审核状态={}, 审核意见={}",
                    userId, pictureId, reviewRequest.getReviewStatus(), reviewRequest.getReviewMessage());
            return true;
        } catch (Exception e) {
            log.error("管理员审核图片失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "审核图片失败: " + e.getMessage());
        }
    }

    /**
     * 管理员批量审核图片
     *
     * @param reviewRequest 审核请求
     * @param userId        操作用户ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean adminBatchReviewPictures(PictureReviewRequest reviewRequest, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(reviewRequest), ErrorCode.PARAMS_ERROR, "审核参数不能为空");
        List<Long> pictureIds = reviewRequest.getPictureIds();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIds), ErrorCode.PARAMS_ERROR, "图片ID列表不能为空");

        try {
            // 实现批量审核更新，需要在PictureMapper中添加批量审核方法
            int result = pictureMapper.batchReview(
                    pictureIds,
                    reviewRequest.getReviewStatus().toString(),
                    reviewRequest.getReviewMessage(),
                    userId,
                    LocalDateTime.now()
            );
            pictureCacheManager.invalidateAfterReviewStatusChange();


            log.info("管理员 {} 批量审核图片成功: 影响行数={}, 图片数量={}, 审核状态={}",
                    userId, result, pictureIds.size(), reviewRequest.getReviewStatus());
            return true;
        } catch (Exception e) {
            log.error("管理员批量审核图片失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量审核图片失败: " + e.getMessage());
        }
    }


    /**
     * 获取图片统计数据
     *
     * @return 图片统计数据
     */
    @Override
    public PictureStatisticsVO getPictureStatistics() {
        try {
            // 查询当前期间统计数据
            Long todayCount = pictureMapper.countTodayUploads();
            Long weekCount = pictureMapper.countWeekUploads();
            Long monthCount = pictureMapper.countMonthUploads();
            Long pendingCount = pictureMapper.countByReviewStatus(String.valueOf(PictureReviewStatusEnum.REVIEWING.getValue()));
            Long totalCount = pictureMapper.countTotalPictures();

            // 获取浏览量和点赞量数据
            Long totalViewCount = pictureMapper.getTotalViewCount();
            Long totalLikeCount = pictureMapper.getTotalLikeCount();

            // 获取上期数据，用于计算环比增长率
            Long lastWeekCount = pictureMapper.countLastWeekUploads();
            Long lastMonthCount = pictureMapper.countLastMonthUploads();
            Long lastPeriodViewCount = pictureMapper.getLastPeriodViewCount();
            Long lastPeriodLikeCount = pictureMapper.getLastPeriodLikeCount();

            // 处理可能的空值
            todayCount = todayCount != null ? todayCount : 0L;
            weekCount = weekCount != null ? weekCount : 0L;
            monthCount = monthCount != null ? monthCount : 0L;
            pendingCount = pendingCount != null ? pendingCount : 0L;
            totalCount = totalCount != null ? totalCount : 0L;
            totalViewCount = totalViewCount != null ? totalViewCount : 0L;
            totalLikeCount = totalLikeCount != null ? totalLikeCount : 0L;
            lastWeekCount = lastWeekCount != null ? lastWeekCount : 0L;
            lastMonthCount = lastMonthCount != null ? lastMonthCount : 0L;
            lastPeriodViewCount = lastPeriodViewCount != null ? lastPeriodViewCount : 0L;
            lastPeriodLikeCount = lastPeriodLikeCount != null ? lastPeriodLikeCount : 0L;

            // 计算环比增长率
            Double weekUploadGrowthRate = calculateGrowthRate(weekCount, lastWeekCount);
            Double monthUploadGrowthRate = calculateGrowthRate(monthCount, lastMonthCount);

            // 计算浏览量和点赞量的增长率
            // 注意：这里计算的是当前总量与上月同期总量的差值相对于上月同期总量的增长率
            Double viewCountGrowthRate = calculateGrowthRate(totalViewCount, lastPeriodViewCount);
            Double likeCountGrowthRate = calculateGrowthRate(totalLikeCount, lastPeriodLikeCount);

            // 构建并返回统计数据
            PictureStatisticsVO statistics = PictureStatisticsVO.builder()
                    .todayUploadCount(todayCount)
                    .weekUploadCount(weekCount)
                    .weekUploadGrowthRate(weekUploadGrowthRate)
                    .monthUploadCount(monthCount)
                    .monthUploadGrowthRate(monthUploadGrowthRate)
                    .pendingReviewCount(pendingCount)
                    .totalCount(totalCount)
                    .totalViewCount(totalViewCount)
                    .viewCountGrowthRate(viewCountGrowthRate)
                    .totalLikeCount(totalLikeCount)
                    .likeCountGrowthRate(likeCountGrowthRate)
                    .build();

            log.info("获取图片统计数据成功: 总数={}, 待审核={}, 浏览量={}, 点赞量={}",
                    totalCount, pendingCount, totalViewCount, totalLikeCount);

            return statistics;
        } catch (Exception e) {
            log.error("获取图片统计数据失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取图片统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 计算环比增长率
     * @param current 当前值
     * @param previous 上期值
     * @return 增长率(%)，保留一位小数
     */
    private Double calculateGrowthRate(Long current, Long previous) {
        if (current == null) current = 0L;
        if (previous == null || previous == 0L) {
            return current > 0 ? 100.0 : 0.0;
        }

        double rate = ((double) current - previous) / previous * 100;
        // 保留一位小数
        return Math.round(rate * 10) / 10.0;
    }

    /**
     * 格式化文件大小
     *
     * @param size 文件大小(字节)
     * @return 格式化后的文件大小
     */
    private String formatFileSize(Long size) {
        if (size == null || size <= 0) {
            return "0 B";
        }

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        // 限制最大单位为TB
        digitGroups = Math.min(digitGroups, 4);

        // 计算并格式化，保留两位小数
        double formattedSize = size / Math.pow(1024, digitGroups);

        // 创建格式化器
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");

        return df.format(formattedSize) + " " + units[digitGroups];
    }


    /**
     * 标签信息接口类
     */
    @Data
    private static class TagInfo {
        private String name;
        private String color;
    }
}
