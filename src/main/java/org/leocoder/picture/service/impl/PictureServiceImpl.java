package org.leocoder.picture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.leocoder.picture.domain.dto.picture.PictureEditRequest;
import org.leocoder.picture.domain.dto.picture.PictureUploadByBatchRequest;
import org.leocoder.picture.domain.dto.picture.PictureUploadRequest;
import org.leocoder.picture.domain.dto.picture.PictureWaterfallRequest;
import org.leocoder.picture.domain.dto.upload.UploadPictureResult;
import org.leocoder.picture.domain.pojo.Category;
import org.leocoder.picture.domain.pojo.Picture;
import org.leocoder.picture.domain.pojo.PictureHash;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.picture.PictureVO;
import org.leocoder.picture.domain.vo.picture.PictureWaterfallVO;
import org.leocoder.picture.enums.PictureReviewStatusEnum;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.exception.ThrowUtils;
import org.leocoder.picture.manager.crawler.PictureCrawler;
import org.leocoder.picture.manager.crawler.PictureCrawlerManager;
import org.leocoder.picture.manager.upload.FilePictureUpload;
import org.leocoder.picture.manager.upload.PictureUploadTemplate;
import org.leocoder.picture.manager.upload.UrlPictureUpload;
import org.leocoder.picture.mapper.CategoryMapper;
import org.leocoder.picture.mapper.PictureHashMapper;
import org.leocoder.picture.mapper.PictureMapper;
import org.leocoder.picture.mapper.TagMapper;
import org.leocoder.picture.service.*;
import org.leocoder.picture.utils.SnowflakeIdGenerator;
import org.leocoder.picture.utils.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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

    private final  UserReactionService userReactionService;


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
            picture.setReviewMessage("等待管理员审核");
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
                // 新增操作 - 使用雪花算法生成ID
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

        // 增加浏览量（异步更新浏览量可以提高性能，此处简化为同步更新）
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

            // 增加浏览量（可以考虑异步处理以提高响应速度）
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

            // 增加浏览量（可以考虑异步处理以提高响应速度）
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
     * 增加图片浏览量
     *
     * @param pictureId 图片ID
     */
    private void incrementViewCount(Long pictureId) {
        // 此处可以优化为异步更新或批量更新，以提高性能
        pictureMapper.incrementViewCount(pictureId);
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
        // 参数校验与默认值设置
        if (ObjectUtil.isNull(requestParam)) {
            requestParam = new PictureWaterfallRequest();
        }

        Integer pageSize = requestParam.getPageSize();
        if (ObjectUtil.isNull(pageSize) || pageSize <= 0 || pageSize > 100) {
            // 默认每页30条
            pageSize = 30;
        }

        String sortBy = requestParam.getSortBy();
        if (StrUtil.isBlank(sortBy)) {
            // 默认按最新发布排序
            sortBy = "newest";
        }

        try {
            // 查询审核通过的图片
            String reviewStatus = String.valueOf(PictureReviewStatusEnum.PASS.getValue());

            // 查询图片列表
            List<Picture> pictureList = pictureMapper.selectWaterfallPictures(
                    reviewStatus,
                    requestParam.getFormat(),
                    requestParam.getMinWidth(),
                    requestParam.getMinHeight(),
                    requestParam.getUserId(),
                    requestParam.getCategoryId(),
                    requestParam.getTagIds(),
                    requestParam.getKeyword(),
                    sortBy,
                    pageSize
            );

            // 获取总数（用于前端显示总数）
            Long total = pictureMapper.countWaterfallPictures(
                    reviewStatus,
                    requestParam.getFormat(),
                    requestParam.getMinWidth(),
                    requestParam.getMinHeight(),
                    requestParam.getUserId(),
                    requestParam.getCategoryId(),
                    requestParam.getTagIds(),
                    requestParam.getKeyword()
            );

            // 转换为VO列表并填充标签
            List<PictureVO> pictureVOList = new ArrayList<>();
            if (CollUtil.isNotEmpty(pictureList)) {
                pictureVOList = convertAndEnrichPictureList(pictureList);
            }

            // 构建返回对象
            Long lastId = null;
            Object lastValue = null;
            boolean hasMore = false;
            if (CollUtil.isNotEmpty(pictureVOList)) {
                // 获取最后一张图片的ID作为下一页的游标
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
                hasMore = pictureVOList.size() >= pageSize && total > pageSize;
            }

            if (ObjectUtil.isNotNull(loginUser)) {
                try {
                    userReactionService.batchFillPictureUserReactionStatus(pictureVOList, loginUser.getId());
                } catch (Exception e) {
                    log.warn("批量填充图片互动信息失败: {}", e.getMessage());
                }
            }

            log.info("获取瀑布流图片成功: 返回{}条记录, 排序方式={}, 总数={}",
                    pictureVOList.size(), sortBy, total);

            return PictureWaterfallVO.builder()
                    .records(pictureVOList)
                    .hasMore(hasMore)
                    .lastId(lastId)
                    .lastValue(lastValue)
                    .total(total)
                    .build();

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
     * @param requestParam 请求参数（与初始请求一致，确保条件统一）
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

        if (ObjectUtil.isNull(requestParam)) {
            requestParam = new PictureWaterfallRequest();
        }

        Integer pageSize = requestParam.getPageSize();
        if (ObjectUtil.isNull(pageSize) || pageSize <= 0 || pageSize > 100) {
            // 默认每页30条
            pageSize = 30;
        }

        String sortBy = requestParam.getSortBy();
        if (StrUtil.isBlank(sortBy)) {
            // 默认按最新发布排序
            sortBy = "newest";
        }

        try {
            // 查询审核通过的图片
            String reviewStatus = String.valueOf(PictureReviewStatusEnum.PASS.getValue());

            // 查询更多图片列表
            List<Picture> pictureList = pictureMapper.selectMoreWaterfallPictures(
                    lastId,
                    lastValue,
                    reviewStatus,
                    requestParam.getFormat(),
                    requestParam.getMinWidth(),
                    requestParam.getMinHeight(),
                    requestParam.getUserId(),
                    requestParam.getCategoryId(),
                    requestParam.getTagIds(),
                    requestParam.getKeyword(),
                    sortBy,
                    pageSize
            );

            // 获取总数（用于前端显示总数）
            Long total = pictureMapper.countWaterfallPictures(
                    reviewStatus,
                    requestParam.getFormat(),
                    requestParam.getMinWidth(),
                    requestParam.getMinHeight(),
                    requestParam.getUserId(),
                    requestParam.getCategoryId(),
                    requestParam.getTagIds(),
                    requestParam.getKeyword()
            );

            // 转换为VO列表并填充标签
            List<PictureVO> pictureVOList = new ArrayList<>();
            if (CollUtil.isNotEmpty(pictureList)) {
                pictureVOList = convertAndEnrichPictureList(pictureList);
            }

            // 构建返回对象
            Long newLastId = null;
            Object newLastValue = null;
            boolean hasMore = false;
            if (CollUtil.isNotEmpty(pictureVOList)) {
                // 获取最后一张图片的ID和排序值作为下一页的游标
                PictureVO lastPicture = pictureVOList.get(pictureVOList.size() - 1);
                newLastId = lastPicture.getId();

                // 根据排序方式获取对应的排序值
                switch (sortBy) {
                    case "newest":
                        newLastValue = lastPicture.getCreateTime();
                        break;
                    case "popular":
                    case "mostViewed":
                        newLastValue = lastPicture.getViewCount();
                        break;
                    case "mostLiked":
                        newLastValue = lastPicture.getLikeCount();
                        break;
                    case "mostCollected":
                        newLastValue = lastPicture.getCollectionCount();
                        break;
                    default:
                        newLastValue = lastPicture.getCreateTime();
                }

                // 判断是否还有更多数据（简单判断，如果返回的数量等于请求的数量，认为还有更多）
                hasMore = pictureVOList.size() >= pageSize;
            }

            log.info("加载更多瀑布流图片成功: 返回{}条记录, 排序方式={}, 总数={}",
                    pictureVOList.size(), sortBy, total);

            if(ObjectUtil.isNotNull(loginUser)){
                try {
                    userReactionService.batchFillPictureUserReactionStatus(pictureVOList, loginUser.getId());
                } catch (Exception e) {
                    log.warn("批量填充图片互动信息失败: {}", e.getMessage());
                }
            }

            return PictureWaterfallVO.builder()
                    .records(pictureVOList)
                    .hasMore(hasMore)
                    .lastId(newLastId)
                    .lastValue(newLastValue)
                    // 添加total属性
                    .total(total)
                    .build();

        } catch (Exception e) {
            log.error("加载更多瀑布流图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加载更多瀑布流图片失败: " + e.getMessage());
        }
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
            // 2. 更新分类关联
            if (requestParam.getCategoryId() != null && requestParam.getCategoryId() > 0) {
                // 获取原分类关联
                List<Long> oldCategoryIds = categoryRelationService.getCategoryIdsByContent("picture", pictureId);
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

            return true;
        } catch (Exception e) {
            log.error("更新图片关联关系失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图片关联关系失败: " + e.getMessage());
        }
    }
}
