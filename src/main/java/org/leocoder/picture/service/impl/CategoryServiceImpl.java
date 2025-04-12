package org.leocoder.picture.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.common.PageUtils;
import org.leocoder.picture.domain.dto.category.*;
import org.leocoder.picture.domain.mapstruct.CategoryConvert;
import org.leocoder.picture.domain.pojo.Category;
import org.leocoder.picture.domain.vo.category.CategoryStatisticsVO;
import org.leocoder.picture.domain.vo.category.CategoryTreeVO;
import org.leocoder.picture.domain.vo.category.CategoryVO;
import org.leocoder.picture.domain.vo.category.RelatedItemVO;
import org.leocoder.picture.enums.CategoryStatusEnum;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.mapper.CategoryMapper;
import org.leocoder.picture.mapper.CategoryRelationMapper;
import org.leocoder.picture.service.CategoryRelationService;
import org.leocoder.picture.service.CategoryService;
import org.leocoder.picture.service.UserService;
import org.leocoder.picture.utils.SnowflakeIdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : 程序员Leo
 * @date  2025-04-08 13:59
 * @version 1.0
 * @description : 分类服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    private final CategoryRelationService categoryRelationService;

    private final UserService userService;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private final CategoryRelationMapper categoryRelationMapper;


    /**
     * 检查urlName是否已存在
     * @param urlName 分类别名
     * @param excludeId 排除的分类ID（用于更新时排除自身）
     * @return 是否存在
     */
    private boolean checkUrlNameExists(String urlName, Long excludeId) {
        if (StrUtil.isBlank(urlName)) {
            return false;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("urlName", urlName);
        params.put("excludeId", excludeId);

        Integer count = categoryMapper.countByUrlName(params);
        return count != null && count > 0;
    }

    /**
     * 创建分类
     *
     * @param categoryCreateRequest 分类创建请求
     * @return 新创建的分类ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CategoryCreateRequest categoryCreateRequest) {
        // 1. 参数校验
        if (ObjectUtil.isNull(categoryCreateRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 检查分类名称是否已存在
        String name = categoryCreateRequest.getName();
        Long parentId = categoryCreateRequest.getParentId();
        String type = categoryCreateRequest.getType();

        if (StrUtil.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类名称不能为空");
        }

        if (StrUtil.isBlank(type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类类型不能为空");
        }


        // 如果parentId为null，设置为0表示顶级分类
        if (ObjectUtil.isNull(parentId)) {
            parentId = 0L;
        }

        // 检查同级下是否有同名分类
        if (checkCategoryNameExists(name, parentId, type, null)) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_EXIST, "该分类名称已存在");
        }

        // 3. 设置分类层级和路径
        // 默认为1级
        Integer level = 1;
        // 默认路径
        String path = "";

        // 如果有父分类，设置正确的层级和路径
        if (parentId > 0) {
            Category parentCategory = categoryMapper.selectById(parentId);
            if (ObjectUtil.isNull(parentCategory) || Boolean.TRUE.equals(parentCategory.getIsDeleted())) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "父分类不存在");
            }

            // 设置层级为父分类层级+1
            level = parentCategory.getLevel() + 1;

            // 设置路径为父分类路径+父分类ID
            path = parentCategory.getPath() + parentId + "-";
        }

        // 4. 生成ID并构建分类对象
        Long categoryId = snowflakeIdGenerator.nextId();
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        // 设置状态，如果未提供则使用默认值ACTIVE
        String status = categoryCreateRequest.getStatus();
        if (StrUtil.isBlank(status)) {
            status = CategoryStatusEnum.ACTIVE.getValue();
        }

        // 设置排序，如果未提供则使用默认值0
        Integer sortOrder = categoryCreateRequest.getSortOrder();
        if (ObjectUtil.isNull(sortOrder)) {
            sortOrder = 0;
        }

        // 处理 urlName 字段
        String urlName = categoryCreateRequest.getUrlName();
        if (StrUtil.isBlank(urlName)) {
            // 生成唯一的 urlName
            // 可以基于分类名称、类型和时间戳
            String baseUrlName = name.toLowerCase()
                    // 将空格替换为连字符
                    .replaceAll("\\s+", "-")
                    // 移除特殊字符
                    .replaceAll("[^a-z0-9\\-]", "");

            // 如果处理后为空，使用拼音或其他替代方案
            if (StrUtil.isBlank(baseUrlName)) {
                baseUrlName = "category";
            }

            // 添加时间戳确保唯一性
            urlName = baseUrlName + "-" + System.currentTimeMillis();

            // 如果urlName太长，可以截取
            if (urlName.length() > 255) {
                urlName = urlName.substring(0, 250) + "-" + System.currentTimeMillis() % 10000;
            }
        } else {
            // 验证自定义urlName的唯一性
            if (checkUrlNameExists(urlName, categoryId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类别名已存在，请更换");
            }
        }

        Category category = Category.builder()
                .id(categoryId)
                .name(name)
                .parentId(parentId)
                .type(type)
                .level(level)
                .path(path)
                .description(categoryCreateRequest.getDescription())
                .icon(categoryCreateRequest.getIcon())
                .urlName(categoryCreateRequest.getUrlName())
                .sortOrder(sortOrder)
                // 新分类内容数量为0
                .contentCount(0)
                .status(status)
                .createTime(now)
                .createUser(userId)
                .updateTime(now)
                .updateUser(userId)
                .isDeleted(0)
                .build();


        // 5. 插入数据库
        int result = categoryMapper.insertWithId(category);
        if (result != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建分类失败，请稍后重试");
        }

        // 6. 返回新分类ID
        return categoryId;
    }

    /**
     * 更新分类
     *
     * @param categoryUpdateRequest 分类更新请求
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCategory(CategoryUpdateRequest categoryUpdateRequest) {
        // 1. 参数校验
        if (ObjectUtil.isNull(categoryUpdateRequest) || ObjectUtil.isNull(categoryUpdateRequest.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        Long categoryId = categoryUpdateRequest.getId();

        // 2. 检查分类是否存在
        Category existCategory = categoryMapper.selectById(categoryId);
        if (ObjectUtil.isNull(existCategory) || Boolean.TRUE.equals(existCategory.getIsDeleted())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "分类不存在");
        }

        // 3. 检查更新的名称是否在同级下重复
        String newName = categoryUpdateRequest.getName();
        if (StrUtil.isNotBlank(newName) && !newName.equals(existCategory.getName())) {
            if (checkCategoryNameExists(newName, existCategory.getParentId(), existCategory.getType(), categoryId)) {
                throw new BusinessException(ErrorCode.CATEGORY_NAME_EXIST, "该分类名称已存在");
            }
        }

        // 4. 构建更新对象
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        Category updateCategory = new Category();
        updateCategory.setId(categoryId);

        // 只更新非空字段
        if (StrUtil.isNotBlank(newName)) {
            updateCategory.setName(newName);
        }

        if (StrUtil.isNotBlank(categoryUpdateRequest.getDescription())) {
            updateCategory.setDescription(categoryUpdateRequest.getDescription());
        }

        if (StrUtil.isNotBlank(categoryUpdateRequest.getIcon())) {
            updateCategory.setIcon(categoryUpdateRequest.getIcon());
        }

        if (StrUtil.isNotBlank(categoryUpdateRequest.getUrlName())) {
            updateCategory.setUrlName(categoryUpdateRequest.getUrlName());
        }

        if (ObjectUtil.isNotNull(categoryUpdateRequest.getSortOrder())) {
            updateCategory.setSortOrder(categoryUpdateRequest.getSortOrder());
        }

        if (StrUtil.isNotBlank(categoryUpdateRequest.getStatus())) {
            updateCategory.setStatus(categoryUpdateRequest.getStatus());
        }

        updateCategory.setUpdateTime(now);
        updateCategory.setUpdateUser(userId);

        // 5. 执行更新
        int result = categoryMapper.updateByPrimaryKeySelective(updateCategory);
        if (result != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新分类失败，请稍后重试");
        }

        return true;
    }

    /**
     * 根据ID删除分类
     *
     * @param id 分类ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCategory(Long id) {
        // 1. 参数校验
        if (ObjectUtil.isNull(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }

        // 2. 检查分类是否存在
        Category category = categoryMapper.selectById(id);
        if (ObjectUtil.isNull(category) || Boolean.TRUE.equals(category.getIsDeleted())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "分类不存在");
        }

        // 3. 检查是否有子分类
        List<Category> children = categoryMapper.selectByParentId(id);
        if (CollUtil.isNotEmpty(children)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该分类下有子分类，无法删除");
        }

        // 4. 检查分类下是否有关联内容
        Integer contentCount = category.getContentCount();
        if (contentCount != null && contentCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该分类下有关联内容，无法删除");
        }

        // 5. 执行逻辑删除
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        int result = categoryMapper.logicDeleteCategory(id, now, userId);
        if (result != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除分类失败，请稍后重试");
        }

        // 6. 删除分类关系
        categoryRelationService.deleteAllRelationsByCategory(id);

        return true;
    }

    /**
     * 批量删除分类
     *
     * @param ids 分类ID列表
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDeleteCategories(List<Long> ids) {
        // 1. 参数校验
        if (CollUtil.isEmpty(ids)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID列表不能为空");
        }

        // 2. 逐个检查分类是否可以删除
        List<Long> validIds = new ArrayList<>();
        for (Long id : ids) {
            Category category = categoryMapper.selectById(id);

            // 跳过不存在或已删除的分类
            if (ObjectUtil.isNull(category) || Boolean.TRUE.equals(category.getIsDeleted())) {
                continue;
            }

            // 检查是否有子分类
            List<Category> children = categoryMapper.selectByParentId(id);
            if (CollUtil.isNotEmpty(children)) {
                continue; // 有子分类，跳过
            }

            // 检查是否有关联内容
            Integer contentCount = category.getContentCount();
            if (contentCount != null && contentCount > 0) {
                continue; // 有关联内容，跳过
            }

            validIds.add(id);
        }

        // 如果没有可删除的分类，直接返回成功
        if (CollUtil.isEmpty(validIds)) {
            return true;
        }

        // 3. 批量逻辑删除
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        int result = categoryMapper.batchLogicDeleteCategories(validIds, now, userId);
        if (result < 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量删除分类失败，请稍后重试");
        }

        // 4. 批量删除分类关系
        categoryRelationService.batchDeleteAllRelationsByCategories(validIds);

        return true;
    }

    /**
     * 根据ID获取分类
     *
     * @param id 分类ID
     * @return 分类VO
     */
    @Override
    public CategoryVO getCategoryById(Long id) {
        // 1. 参数校验
        if (ObjectUtil.isNull(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }

        // 2. 查询分类
        Category category = categoryMapper.selectById(id);
        if (ObjectUtil.isNull(category) || Boolean.TRUE.equals(category.getIsDeleted())) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "分类不存在");
        }

        // 3. 获取父分类名称
        String parentName = "";
        if (category.getParentId() > 0) {
            Category parentCategory = categoryMapper.selectById(category.getParentId());
            if (ObjectUtil.isNotNull(parentCategory) && !Boolean.TRUE.equals(parentCategory.getIsDeleted())) {
                parentName = parentCategory.getName();
            }
        }

        // 4. 创建者和更新者名称
        String createUsername = "";
        String updateUsername = "";

        if (ObjectUtil.isNotNull(category.getCreateUser())) {
            try {
                createUsername = userService.getUserVOById(category.getCreateUser()).getUsername();
            } catch (Exception e) {
                log.error("获取创建者信息失败", e);
            }
        }

        if (ObjectUtil.isNotNull(category.getUpdateUser())) {
            try {
                updateUsername = userService.getUserVOById(category.getUpdateUser()).getUsername();
            } catch (Exception e) {
                log.error("获取更新者信息失败", e);
            }
        }

        // 5. 构建并返回VO
        return CategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParentId())
                .parentName(parentName)
                .type(category.getType())
                .level(category.getLevel())
                .path(category.getPath())
                .description(category.getDescription())
                .icon(category.getIcon())
                .urlName(category.getUrlName())
                .sortOrder(category.getSortOrder())
                .contentCount(category.getContentCount())
                .status(category.getStatus())
                .createTime(category.getCreateTime())
                .createUser(category.getCreateUser())
                .createUsername(createUsername)
                .updateTime(category.getUpdateTime())
                .updateUser(category.getUpdateUser())
                .updateUsername(updateUsername)
                .build();
    }

    /**
     * 分页查询分类列表
     *
     * @param requestParam 分类查询请求
     * @return 分页结果
     */
    @Override
    public PageResult<CategoryVO> listCategoryByPage(CategoryQueryRequest requestParam) {
        // 解析日期参数
        LocalDateTime createTimeStart = null;
        if (StrUtil.isNotBlank(requestParam.getCreateTimeStart())) {
            try {
                createTimeStart = LocalDateTime.parse(requestParam.getCreateTimeStart(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e) {
                log.error("日期格式解析错误", e);
            }
        }

        LocalDateTime createTimeEnd = null;
        if (StrUtil.isNotBlank(requestParam.getCreateTimeEnd())) {
            try {
                createTimeEnd = LocalDateTime.parse(requestParam.getCreateTimeEnd(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e) {
                log.error("日期格式解析错误", e);
            }
        }

        // 使用 createTimeStart 和 createTimeEnd 的最终值
        final LocalDateTime finalCreateTimeStart = createTimeStart;
        final LocalDateTime finalCreateTimeEnd = createTimeEnd;
        // 使用 PageUtils 和 MapStruct 进行分页查询
        return PageUtils.doPage(
                requestParam,
                () -> categoryMapper.listCategoriesByPage(
                        requestParam, finalCreateTimeStart, finalCreateTimeEnd),
                category -> {
                    // 基本属性转换
                    CategoryVO vo = CategoryConvert.INSTANCE.toCategoryVO(category);

                    // 设置父分类名称
                    if (category.getParentId() != null && category.getParentId() > 0) {
                        try {
                            Category parentCategory = categoryMapper.selectById(category.getParentId());
                            if (parentCategory != null) {
                                vo.setParentName(parentCategory.getName());
                            }
                        } catch (Exception e) {
                            log.error("获取父分类信息失败, parentId={}", category.getParentId(), e);
                        }
                    }

                    // 设置创建人和更新人信息
                    if (category.getCreateUser() != null) {
                        try {
                            vo.setCreateUsername(userService.getUserVOById(category.getCreateUser()).getUsername());
                        } catch (Exception e) {
                            log.error("获取创建者信息失败, userId={}", category.getCreateUser(), e);
                        }
                    }

                    if (category.getUpdateUser() != null) {
                        try {
                            vo.setUpdateUsername(userService.getUserVOById(category.getUpdateUser()).getUsername());
                        } catch (Exception e) {
                            log.error("获取更新者信息失败, userId={}", category.getUpdateUser(), e);
                        }
                    }

                    return vo;
                }
        );
    }

    /**
     * 获取分类树形结构
     *
     * @param type 分类类型
     * @return 分类树形结构列表
     */
    @Override
    public List<CategoryTreeVO> getCategoryTree(String type) {
        // 1. 查询指定类型的所有分类
        List<Category> allCategories = categoryMapper.selectByType(type);

        // 过滤掉已删除的分类
        allCategories = allCategories.stream()
                .filter(category -> !Boolean.TRUE.equals(category.getIsDeleted()))
                .collect(Collectors.toList());

        // 如果没有数据，返回空列表
        if (CollUtil.isEmpty(allCategories)) {
            return Collections.emptyList();
        }

        // 2. 转换为树形结构

        // 先转换为Map，便于查找
        Map<Long, CategoryTreeVO> categoryMap = new HashMap<>();

        // 创建所有节点
        for (Category category : allCategories) {
            CategoryTreeVO treeNode = CategoryTreeVO.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .parentId(category.getParentId())
                    .type(category.getType())
                    .level(category.getLevel())
                    .path(category.getPath())
                    .description(category.getDescription())
                    .icon(category.getIcon())
                    .urlName(category.getUrlName())
                    .sortOrder(category.getSortOrder())
                    .contentCount(category.getContentCount())
                    .status(category.getStatus())
                    .children(new ArrayList<>())
                    .build();

            categoryMap.put(category.getId(), treeNode);
        }

        // 顶级分类列表
        List<CategoryTreeVO> rootCategories = new ArrayList<>();

        // 构建树形结构
        for (Category category : allCategories) {
            CategoryTreeVO current = categoryMap.get(category.getId());

            // 如果是顶级分类，加入结果列表
            if (category.getParentId() == null || category.getParentId() == 0) {
                rootCategories.add(current);
            } else {
                // 不是顶级分类，将其添加到父分类的children列表中
                CategoryTreeVO parent = categoryMap.get(category.getParentId());
                if (parent != null) {
                    parent.getChildren().add(current);
                } else {
                    // 父分类不存在（可能已被删除），作为顶级分类处理
                    rootCategories.add(current);
                }
            }
        }

        // 3. 对树形结构进行排序（按sortOrder正序排列）
        sortCategoryTree(rootCategories);

        return rootCategories;
    }

    /**
     * 递归排序分类树
     */
    private void sortCategoryTree(List<CategoryTreeVO> categoryList) {
        if (CollUtil.isEmpty(categoryList)) {
            return;
        }

        // 按sortOrder正序排列
        categoryList.sort(Comparator.comparing(CategoryTreeVO::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())));

        // 递归排序子分类
        for (CategoryTreeVO category : categoryList) {
            if (CollUtil.isNotEmpty(category.getChildren())) {
                sortCategoryTree(category.getChildren());
            }
        }
    }

    /**
     * 移动分类
     *
     * @param categoryMoveRequest 分类移动请求
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean moveCategory(CategoryMoveRequest categoryMoveRequest) {
        // 1. 参数校验
        if (ObjectUtil.isNull(categoryMoveRequest) ||
                ObjectUtil.isNull(categoryMoveRequest.getId()) ||
                ObjectUtil.isNull(categoryMoveRequest.getParentId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        Long categoryId = categoryMoveRequest.getId();
        Long newParentId = categoryMoveRequest.getParentId();

        // 分类不能移动到自己下面
        if (categoryId.equals(newParentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能将分类移动到自身下");
        }

        // 2. 检查分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (ObjectUtil.isNull(category) || Boolean.TRUE.equals(category.getIsDeleted())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "分类不存在");
        }

        // 3. 如果新父ID不为0，检查父分类是否存在
        if (newParentId > 0) {
            Category parentCategory = categoryMapper.selectById(newParentId);
            if (ObjectUtil.isNull(parentCategory) || Boolean.TRUE.equals(parentCategory.getIsDeleted())) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "父分类不存在");
            }

            // 分类不能移动到自己的子分类下
            if (isChildCategory(newParentId, categoryId)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能将分类移动到其子分类下");
            }

            // 检查同级下是否有同名分类
            if (checkCategoryNameExists(category.getName(), newParentId, category.getType(), categoryId)) {
                throw new BusinessException(ErrorCode.CATEGORY_NAME_EXIST, "目标位置已存在同名分类");
            }

            // 4. 计算新的层级和路径
            Integer newLevel = parentCategory.getLevel() + 1;
            String newPath = parentCategory.getPath() + newParentId + "-";

            // 5. 更新分类
            Long userId = StpUtil.getLoginIdAsLong();
            LocalDateTime now = LocalDateTime.now();

            Category updateCategory = new Category();
            updateCategory.setId(categoryId);
            updateCategory.setParentId(newParentId);
            updateCategory.setLevel(newLevel);
            updateCategory.setPath(newPath);
            updateCategory.setUpdateTime(now);
            updateCategory.setUpdateUser(userId);

            int result = categoryMapper.updateByPrimaryKeySelective(updateCategory);
            if (result != 1) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "移动分类失败，请稍后重试");
            }

            // 6. 更新子分类的层级和路径
            updateChildrenLevelAndPath(categoryId, newLevel, newPath);
        } else {
            // 移动到顶级目录

            // 检查顶级目录下是否有同名分类
            if (checkCategoryNameExists(category.getName(), 0L, category.getType(), categoryId)) {
                throw new BusinessException(ErrorCode.CATEGORY_NAME_EXIST, "目标位置已存在同名分类");
            }

            // 设置为顶级分类
            Long userId = StpUtil.getLoginIdAsLong();
            LocalDateTime now = LocalDateTime.now();

            Category updateCategory = new Category();
            updateCategory.setId(categoryId);
            updateCategory.setParentId(0L);
            // 顶级为1级
            updateCategory.setLevel(1);
            // 顶级路径为空
            updateCategory.setPath("");
            updateCategory.setUpdateTime(now);
            updateCategory.setUpdateUser(userId);

            int result = categoryMapper.updateByPrimaryKeySelective(updateCategory);
            if (result != 1) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "移动分类失败，请稍后重试");
            }

            // 更新子分类的层级和路径
            updateChildrenLevelAndPath(categoryId, 1, "");
        }

        return true;
    }

    /**
     * 检查是否为子分类
     *
     * @param categoryId 当前分类ID
     * @param possibleParentId 可能的父分类ID
     * @return 是否为子分类
     */
    private boolean isChildCategory(Long categoryId, Long possibleParentId) {
        Category category = categoryMapper.selectById(categoryId);
        if (ObjectUtil.isNull(category)) {
            return false;
        }

        if (category.getParentId().equals(possibleParentId)) {
            return true;
        }

        if (category.getParentId() > 0) {
            return isChildCategory(category.getParentId(), possibleParentId);
        }

        return false;
    }

    /**
     * 更新子分类的层级和路径
     *
     * @param parentId 父分类ID
     * @param parentLevel 父分类层级
     * @param parentPath 父分类路径
     */
    private void updateChildrenLevelAndPath(Long parentId, Integer parentLevel, String parentPath) {
        // 查询所有子分类
        List<Category> children = categoryMapper.selectByParentId(parentId);
        if (CollUtil.isEmpty(children)) {
            return;
        }

        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        for (Category child : children) {
            // 计算新的层级和路径
            Integer newLevel = parentLevel + 1;
            String newPath = parentPath + parentId + "-";

            // 更新子分类
            Category updateChild = new Category();
            updateChild.setId(child.getId());
            updateChild.setLevel(newLevel);
            updateChild.setPath(newPath);
            updateChild.setUpdateTime(now);
            updateChild.setUpdateUser(userId);

            categoryMapper.updateByPrimaryKeySelective(updateChild);

            // 递归更新其子分类
            updateChildrenLevelAndPath(child.getId(), newLevel, newPath);
        }
    }

    /**
     * 批量移动分类
     *
     * @param batchCategoryMoveRequest 批量分类移动请求
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchMoveCategories(BatchCategoryMoveRequest batchCategoryMoveRequest) {
        // 1. 参数校验
        if (ObjectUtil.isNull(batchCategoryMoveRequest) ||
                CollUtil.isEmpty(batchCategoryMoveRequest.getIds()) ||
                ObjectUtil.isNull(batchCategoryMoveRequest.getParentId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        List<Long> categoryIds = batchCategoryMoveRequest.getIds();
        Long newParentId = batchCategoryMoveRequest.getParentId();

        // 分类不能移动到自己下面
        if (categoryIds.contains(newParentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能将分类移动到自身下");
        }

        // 2. 如果新父ID不为0，检查父分类是否存在
        if (newParentId > 0) {
            Category parentCategory = categoryMapper.selectById(newParentId);
            if (ObjectUtil.isNull(parentCategory) || Boolean.TRUE.equals(parentCategory.getIsDeleted())) {
                throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "父分类不存在");
            }
        }

        // 3. 逐个移动分类
        for (Long categoryId : categoryIds) {
            CategoryMoveRequest moveRequest = new CategoryMoveRequest();
            moveRequest.setId(categoryId);
            moveRequest.setParentId(newParentId);

            try {
                moveCategory(moveRequest);
            } catch (BusinessException e) {
                log.error("批量移动分类失败，categoryId={}, newParentId={}, error={}",
                        categoryId, newParentId, e.getMessage());
                // 继续处理下一个
            }
        }

        return true;
    }

    /**
     * 获取分类统计信息
     *
     * @return 分类统计信息
     */
    @Override
    public CategoryStatisticsVO getCategoryStatistics() {
        // 1. 获取基础统计数据
        Long totalCategories = categoryMapper.countCategories(null, null, null);
        Long newCategoriesOfToday = categoryMapper.countNewCategoriesOfToday();
        Long newCategoriesOfMonth = categoryMapper.countNewCategoriesOfMonth();
        Long topLevelCategories = categoryMapper.countTopLevelCategories(null);
        Integer maxCategoryLevel = categoryMapper.getMaxCategoryLevel();

        // 2. 计算增长率
        Long lastMonthTotalCategories = categoryMapper.countLastMonthTotalCategories();
        Double categoryGrowthRate = 0.0;
        if (ObjectUtil.isNotNull(lastMonthTotalCategories) && lastMonthTotalCategories > 0) {
            categoryGrowthRate = ((totalCategories - lastMonthTotalCategories) * 100.0) / lastMonthTotalCategories;
            // 保留两位小数
            categoryGrowthRate = Math.round(categoryGrowthRate * 100.0) / 100.0;
        }

        // 3. 获取内容最多的分类
        Category mostContentsCategory = categoryMapper.getMostContentsCategory();
        Long mostContentsCategoryId = null;
        String mostContentsCategoryName = "";
        Integer mostContentsCount = 0;

        if (ObjectUtil.isNotNull(mostContentsCategory)) {
            mostContentsCategoryId = mostContentsCategory.getId();
            mostContentsCategoryName = mostContentsCategory.getName();
            mostContentsCount = mostContentsCategory.getContentCount();
        }

        // 4. 获取激活和禁用分类数量
        Long activeCategories = categoryMapper.countActiveCategories();
        Long disabledCategories = categoryMapper.countDisabledCategories();

        // 5. 构建并返回统计VO
        return CategoryStatisticsVO.builder()
                .totalCategories(totalCategories)
                .newCategoriesOfToday(newCategoriesOfToday)
                .newCategoriesOfMonth(newCategoriesOfMonth)
                .topLevelCategories(topLevelCategories)
                .maxCategoryLevel(maxCategoryLevel)
                .categoryGrowthRate(categoryGrowthRate)
                .mostContentsCategory(mostContentsCategoryId)
                .mostContentsCategoryName(mostContentsCategoryName)
                .mostContentsCount(mostContentsCount)
                .activeCategories(activeCategories)
                .disabledCategories(disabledCategories)
                .build();
    }

    /**
     * 获取分类关联的内容列表
     *
     * @param categoryId 分类ID
     * @param contentType 内容类型
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @Override
    public PageResult<RelatedItemVO> getCategoryRelatedItems(Long categoryId, String contentType, Integer pageNum, Integer pageSize) {
        // 1. 参数校验
        if (ObjectUtil.isNull(categoryId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }

        // 检查分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (ObjectUtil.isNull(category) || Boolean.TRUE.equals(category.getIsDeleted())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "分类不存在");
        }

        // 2. 设置分页参数
        if (ObjectUtil.isNull(pageNum) || pageNum < 1) {
            pageNum = 1;
        }

        if (ObjectUtil.isNull(pageSize) || pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        // 计算分页起始位置
        int offset = (pageNum - 1) * pageSize;

        // 3. 查询总数
        Long total = Long.valueOf(categoryRelationService.countContentsByCategory(categoryId, contentType));
        if (total == 0) {
            return PageResult.build(0L, Collections.emptyList(), pageNum, pageSize);
        }

        // 4. 查询分页数据
        List<RelatedItemVO> relatedItems = categoryRelationMapper.selectCategoryRelatedItems(
                categoryId, contentType, offset, pageSize);

        // 5. 返回分页结果
        return PageResult.build(total, relatedItems, pageNum, pageSize);
    }

    /**
     * 导出分类数据
     *
     * @param categoryExportRequest 分类导出请求
     * @return 导出文件URL
     */
    @Override
    public String exportCategories(CategoryExportRequest categoryExportRequest) {
        // 本方法需要根据实际项目情况进行实现
        // 通常涉及到创建Excel文件、保存文件、生成下载链接等步骤
        // 这里提供一个简单的实现思路

        // 1. 参数校验
        if (ObjectUtil.isNull(categoryExportRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 确定导出哪些分类
        List<Category> categoriesToExport = new ArrayList<>();

        if (Boolean.TRUE.equals(categoryExportRequest.getExportAll())) {
            // 导出所有分类
            categoryMapper.selectByType(null).stream()
                    .filter(category -> !Boolean.TRUE.equals(category.getIsDeleted()))
                    .forEach(categoriesToExport::add);
        } else if (CollUtil.isNotEmpty(categoryExportRequest.getIds())) {
            // 导出指定ID的分类
            for (Long id : categoryExportRequest.getIds()) {
                Category category = categoryMapper.selectById(id);
                if (ObjectUtil.isNotNull(category) && !Boolean.TRUE.equals(category.getIsDeleted())) {
                    categoriesToExport.add(category);
                }
            }
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择要导出的分类");
        }

        if (CollUtil.isEmpty(categoriesToExport)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有可导出的分类数据");
        }

        // 3. 根据导出格式处理数据
        String format = categoryExportRequest.getFormat();
        if (StrUtil.isBlank(format)) {
            format = "xlsx"; // 默认为Excel格式
        }

        // 这里应该调用文件处理服务，将分类数据导出为指定格式的文件
        // 由于涉及到实际的文件处理逻辑，这里只做模拟
        String fileUrl = "/api/download/categories_" + System.currentTimeMillis() + "." + format;

        return fileUrl;
    }

    /**
     * 获取分类列表(不分页)
     *
     * @param type 分类类型
     * @return 分类VO列表
     */
    @Override
    public List<CategoryVO> listCategoriesByType(String type) {
        // 1. 查询指定类型的所有分类
        List<Category> categories = categoryMapper.selectByType(type);

        // 过滤掉已删除的分类
        categories = categories.stream()
                .filter(category -> !Boolean.TRUE.equals(category.getIsDeleted()))
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(categories)) {
            return Collections.emptyList();
        }

        // 2. 收集需要查询的父分类ID和用户ID
        Set<Long> parentIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();

        for (Category category : categories) {
            if (category.getParentId() != null && category.getParentId() > 0) {
                parentIds.add(category.getParentId());
            }

            if (category.getCreateUser() != null) {
                userIds.add(category.getCreateUser());
            }

            if (category.getUpdateUser() != null) {
                userIds.add(category.getUpdateUser());
            }
        }

        // 3. 批量查询父分类
        Map<Long, String> parentNameMap = new HashMap<>();
        if (!parentIds.isEmpty()) {
            for (Long parentId : parentIds) {
                Category parentCategory = categoryMapper.selectById(parentId);
                if (ObjectUtil.isNotNull(parentCategory) && !Boolean.TRUE.equals(parentCategory.getIsDeleted())) {
                    parentNameMap.put(parentId, parentCategory.getName());
                }
            }
        }

        // 4. 批量查询用户名
        Map<Long, String> usernameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Long userId : userIds) {
                try {
                    String username = userService.getUserVOById(userId).getUsername();
                    usernameMap.put(userId, username);
                } catch (Exception e) {
                    log.error("获取用户信息失败, userId={}", userId, e);
                }
            }
        }

        // 5. 构建VO列表
        List<CategoryVO> categoryVOList = new ArrayList<>();

        for (Category category : categories) {
            String parentName = "";
            if (category.getParentId() != null && category.getParentId() > 0) {
                parentName = parentNameMap.getOrDefault(category.getParentId(), "");
            }

            String createUsername = "";
            if (category.getCreateUser() != null) {
                createUsername = usernameMap.getOrDefault(category.getCreateUser(), "");
            }

            String updateUsername = "";
            if (category.getUpdateUser() != null) {
                updateUsername = usernameMap.getOrDefault(category.getUpdateUser(), "");
            }

            CategoryVO categoryVO = CategoryVO.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .parentId(category.getParentId())
                    .parentName(parentName)
                    .type(category.getType())
                    .level(category.getLevel())
                    .path(category.getPath())
                    .description(category.getDescription())
                    .icon(category.getIcon())
                    .urlName(category.getUrlName())
                    .sortOrder(category.getSortOrder())
                    .contentCount(category.getContentCount())
                    .status(category.getStatus())
                    .createTime(category.getCreateTime())
                    .createUser(category.getCreateUser())
                    .createUsername(createUsername)
                    .updateTime(category.getUpdateTime())
                    .updateUser(category.getUpdateUser())
                    .updateUsername(updateUsername)
                    .build();

            categoryVOList.add(categoryVO);
        }

        // 6. 按排序字段排序
        categoryVOList.sort(Comparator.comparing(CategoryVO::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())));

        return categoryVOList;
    }

    /**
     * 检查分类名称是否存在
     *
     * @param name 分类名称
     * @param parentId 父分类ID
     * @param type 分类类型
     * @param excludeId 排除的分类ID(更新时使用)
     * @return 是否存在
     */
    @Override
    public Boolean checkCategoryNameExists(String name, Long parentId, String type, Long excludeId) {
        // 查询指定名称和父ID的分类
        Category existCategory = categoryMapper.selectByNameAndParent(name, parentId, type);

        // 如果不存在同名分类，返回false
        if (ObjectUtil.isNull(existCategory) || Boolean.TRUE.equals(existCategory.getIsDeleted())) {
            return false;
        }

        // 如果存在同名分类，但ID等于排除ID，返回false
        if (ObjectUtil.isNotNull(excludeId) && existCategory.getId().equals(excludeId)) {
            return false;
        }

        // 存在同名分类且不是自己，返回true
        return true;
    }

    /**
     * 更新分类内容数量
     *
     * @param categoryId 分类ID
     * @param increment 增量(可为负)
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCategoryContentCount(Long categoryId, Integer increment) {
        // 1. 参数校验
        if (ObjectUtil.isNull(categoryId) || ObjectUtil.isNull(increment)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 检查分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (ObjectUtil.isNull(category) || Boolean.TRUE.equals(category.getIsDeleted())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "分类不存在");
        }

        // 3. 更新内容数量
        LocalDateTime now = LocalDateTime.now();
        int result = categoryMapper.updateContentCount(categoryId, increment, now);

        // 4. 如果有父分类，递归更新父分类的内容数量
        if (category.getParentId() != null && category.getParentId() > 0) {
            updateCategoryContentCount(category.getParentId(), increment);
        }

        return result > 0;
    }

    /**
     * 递归获取所有子分类ID
     *
     * @param categoryId 分类ID
     * @return 子分类ID列表(包含自身)
     */
    @Override
    public List<Long> getAllChildCategoryIds(Long categoryId) {
        List<Long> result = new ArrayList<>();
        // 添加自身
        result.add(categoryId);

        // 查询直接子分类
        List<Category> children = categoryMapper.selectByParentId(categoryId);
        if (CollUtil.isNotEmpty(children)) {
            for (Category child : children) {
                if (!Boolean.TRUE.equals(child.getIsDeleted())) {
                    // 递归获取子分类的子分类
                    result.addAll(getAllChildCategoryIds(child.getId()));
                }
            }
        }

        return result;
    }
}