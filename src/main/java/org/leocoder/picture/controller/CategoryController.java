package org.leocoder.picture.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.domain.dto.category.*;
import org.leocoder.picture.domain.vo.category.CategoryStatisticsVO;
import org.leocoder.picture.domain.vo.category.CategoryTreeVO;
import org.leocoder.picture.domain.vo.category.CategoryVO;
import org.leocoder.picture.domain.vo.category.RelatedItemVO;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 分类管理相关接口
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "分类管理")
public class CategoryController {

    private final CategoryService categoryService;

    @ApiOperation("创建分类")
    @PostMapping("/admin/category/create")
    public Result<Long> createCategory(@RequestBody CategoryCreateRequest categoryCreateRequest) {
        // 参数校验
        if (ObjectUtil.isEmpty(categoryCreateRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 校验分类名称
        String name = categoryCreateRequest.getName();
        if (StrUtil.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类名称不能为空");
        }
        if (name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类名称长度不能超过50个字符");
        }

        // 校验分类类型
        String type = categoryCreateRequest.getType();
        if (StrUtil.isBlank(type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类类型不能为空");
        }
        if (type.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类类型长度不能超过20个字符");
        }

        // 校验父分类ID（可选）
        Long parentId = categoryCreateRequest.getParentId();
        if (ObjectUtil.isNotEmpty(parentId) && parentId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "父分类ID不能为负数");
        }

        // 校验描述（可选）
        String description = categoryCreateRequest.getDescription();
        if (StrUtil.isNotBlank(description) && description.length() > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类描述长度不能超过500个字符");
        }

        // 校验图标（可选）
        String icon = categoryCreateRequest.getIcon();
        if (StrUtil.isNotBlank(icon) && icon.length() > 255) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图标URL长度不能超过255个字符");
        }

        // 校验URL名称（可选）
        String urlName = categoryCreateRequest.getUrlName();
        if (StrUtil.isNotBlank(urlName) && urlName.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL名称长度不能超过50个字符");
        }

        // 校验排序值（可选）
        Integer sortOrder = categoryCreateRequest.getSortOrder();
        if (ObjectUtil.isNotEmpty(sortOrder) && sortOrder < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "排序值不能为负数");
        }

        Long categoryId = categoryService.createCategory(categoryCreateRequest);
        return ResultUtils.success(categoryId);
    }

    @ApiOperation("更新分类")
    @PutMapping("/admin/category/update")
    public Result<Boolean> updateCategory(@RequestBody CategoryUpdateRequest categoryUpdateRequest) {
        // 参数校验
        if (ObjectUtil.isEmpty(categoryUpdateRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 校验分类ID
        Long id = categoryUpdateRequest.getId();
        if (ObjectUtil.isEmpty(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID必须大于0");
        }

        // 校验分类名称（可选）
        String name = categoryUpdateRequest.getName();
        if (StrUtil.isNotBlank(name) && name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类名称长度不能超过50个字符");
        }

        // 校验描述（可选）
        String description = categoryUpdateRequest.getDescription();
        if (StrUtil.isNotBlank(description) && description.length() > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类描述长度不能超过500个字符");
        }

        // 校验图标（可选）
        String icon = categoryUpdateRequest.getIcon();
        if (StrUtil.isNotBlank(icon) && icon.length() > 255) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图标URL长度不能超过255个字符");
        }

        // 校验URL名称（可选）
        String urlName = categoryUpdateRequest.getUrlName();
        if (StrUtil.isNotBlank(urlName) && urlName.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL名称长度不能超过50个字符");
        }

        // 校验排序值（可选）
        Integer sortOrder = categoryUpdateRequest.getSortOrder();
        if (ObjectUtil.isNotEmpty(sortOrder) && sortOrder < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "排序值不能为负数");
        }

        Boolean result = categoryService.updateCategory(categoryUpdateRequest);
        return ResultUtils.success(result);
    }

    @ApiOperation("删除分类")
    @DeleteMapping("/admin/category/delete")
    public Result<Boolean> deleteCategory(@ApiParam("分类ID") @RequestParam Long id) {
        // 参数校验
        if (ObjectUtil.isEmpty(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID必须大于0");
        }
        Boolean result = categoryService.deleteCategory(id);
        return ResultUtils.success(result);
    }

    @ApiOperation("批量删除分类")
    @DeleteMapping("/admin/category/batch/delete")
    public Result<Boolean> batchDeleteCategories(@RequestBody BatchCategoryDeleteRequest batchDeleteRequest) {
        // 参数校验
        if (ObjectUtil.isEmpty(batchDeleteRequest) || ObjectUtil.isEmpty(batchDeleteRequest.getIds())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID列表不能为空");
        }

        // 校验ID列表是否合法
        List<Long> ids = batchDeleteRequest.getIds();
        if (ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID列表不能为空");
        }

        for (Long id : ids) {
            if (id <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID必须大于0");
            }
        }

        Boolean result = categoryService.batchDeleteCategories(batchDeleteRequest.getIds());
        return ResultUtils.success(result);
    }

    @ApiOperation("获取分类详情")
    @GetMapping("/admin/category/get")
    public Result<CategoryVO> getCategoryById(@ApiParam("分类ID") @RequestParam Long id) {
        // 参数校验
        if (ObjectUtil.isEmpty(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID必须大于0");
        }
        CategoryVO categoryVO = categoryService.getCategoryById(id);
        return ResultUtils.success(categoryVO);
    }

    @ApiOperation("分页获取分类列表")
    @GetMapping("/admin/category/list/page")
    public Result<PageResult<CategoryVO>> listCategoryByPage(CategoryQueryRequest categoryQueryRequest) {
        // 参数校验
        if (ObjectUtil.isEmpty(categoryQueryRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 校验分页参数
        Integer pageNum = categoryQueryRequest.getPageNum();
        Integer pageSize = categoryQueryRequest.getPageSize();
        if (ObjectUtil.isNotEmpty(pageNum) && pageNum <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }
        if (ObjectUtil.isNotEmpty(pageSize) && (pageSize <= 0 || pageSize > 100)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页条数必须在1-100之间");
        }

        // 校验时间范围（可选）
        String createTimeStart = categoryQueryRequest.getCreateTimeStart();
        String createTimeEnd = categoryQueryRequest.getCreateTimeEnd();
        if (StrUtil.isNotBlank(createTimeStart) && StrUtil.isNotBlank(createTimeEnd)
                && createTimeStart.compareTo(createTimeEnd) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始时间不能晚于结束时间");
        }

        PageResult<CategoryVO> pageResult = categoryService.listCategoryByPage(categoryQueryRequest);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("获取分类树形结构")
    @GetMapping("/admin/category/tree")
    public Result<List<CategoryTreeVO>> getCategoryTree(@ApiParam("分类类型") @RequestParam(required = false) String type) {
        // 参数校验
        if (StrUtil.isNotBlank(type) && type.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类类型长度不能超过20个字符");
        }

        List<CategoryTreeVO> categoryTree = categoryService.getCategoryTree(type);
        return ResultUtils.success(categoryTree);
    }

    @ApiOperation("移动分类")
    @PostMapping("/admin/category/move")
    public Result<Boolean> moveCategory(@RequestBody CategoryMoveRequest categoryMoveRequest) {
        // 参数校验
        if (ObjectUtil.isEmpty(categoryMoveRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 校验分类ID
        Long id = categoryMoveRequest.getId();
        if (ObjectUtil.isEmpty(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID必须大于0");
        }

        // 校验父分类ID
        Long parentId = categoryMoveRequest.getParentId();
        if (ObjectUtil.isEmpty(parentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "父分类ID不能为空");
        }
        if (parentId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "父分类ID不能为负数");
        }

        // 检查是否移动到自身
        if (id.equals(parentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类不能移动到自身下");
        }

        Boolean result = categoryService.moveCategory(categoryMoveRequest);
        return ResultUtils.success(result);
    }

    @ApiOperation("批量移动分类")
    @PostMapping("/admin/category/batch/move")
    public Result<Boolean> batchMoveCategories(@RequestBody BatchCategoryMoveRequest batchCategoryMoveRequest) {
        // 参数校验
        if (ObjectUtil.isEmpty(batchCategoryMoveRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 校验分类ID列表
        List<Long> ids = batchCategoryMoveRequest.getIds();
        if (CollUtil.isEmpty(ids)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID列表不能为空");
        }

        for (Long id : ids) {
            if (id <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID必须大于0");
            }
        }

        // 校验父分类ID
        Long parentId = batchCategoryMoveRequest.getParentId();
        if (ObjectUtil.isEmpty(parentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "父分类ID不能为空");
        }
        if (parentId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "父分类ID不能为负数");
        }

        // 检查是否有分类移动到自身下
        if (ids.contains(parentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类不能移动到自身下");
        }

        Boolean result = categoryService.batchMoveCategories(batchCategoryMoveRequest);
        return ResultUtils.success(result);
    }

    @ApiOperation("获取分类统计信息")
    @GetMapping("/admin/category/statistics")
    public Result<CategoryStatisticsVO> getCategoryStatistics() {
        CategoryStatisticsVO statisticsVO = categoryService.getCategoryStatistics();
        return ResultUtils.success(statisticsVO);
    }

    @ApiOperation("获取分类关联的内容列表")
    @GetMapping("/admin/category/related/items")
    public Result<PageResult<RelatedItemVO>> getCategoryRelatedItems(
            @ApiParam("分类ID") @RequestParam Long categoryId,
            @ApiParam("内容类型") @RequestParam(required = false) String contentType,
            @ApiParam("页码") @RequestParam(required = false) Integer pageNum,
            @ApiParam("每页大小") @RequestParam(required = false) Integer pageSize) {

        // 参数校验
        if (ObjectUtil.isEmpty(categoryId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }
        if (categoryId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID必须大于0");
        }

        // 校验内容类型（可选）
        if (StrUtil.isNotBlank(contentType) && contentType.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过20个字符");
        }

        // 校验分页参数
        if (ObjectUtil.isNotEmpty(pageNum) && pageNum <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }
        if (ObjectUtil.isNotEmpty(pageSize) && (pageSize <= 0 || pageSize > 100)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页条数必须在1-100之间");
        }

        PageResult<RelatedItemVO> pageResult = categoryService.getCategoryRelatedItems(categoryId, contentType, pageNum, pageSize);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("导出分类数据")
    @PostMapping("/admin/category/export")
    public Result<String> exportCategories(@RequestBody CategoryExportRequest categoryExportRequest) {
        // 参数校验
        if (ObjectUtil.isEmpty(categoryExportRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 校验导出全部或指定ID列表
        Boolean exportAll = categoryExportRequest.getExportAll();
        List<Long> ids = categoryExportRequest.getIds();

        if (Boolean.FALSE.equals(exportAll) && CollUtil.isEmpty(ids)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "导出全部为false时，必须指定分类ID列表");
        }

        // 校验ID列表（如果有）
        if (CollUtil.isNotEmpty(ids)) {
            for (Long id : ids) {
                if (id <= 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID必须大于0");
                }
            }
        }

        // 校验导出格式
        String format = categoryExportRequest.getFormat();
        if (StrUtil.isBlank(format)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "导出格式不能为空");
        }

        // 检查导出格式是否支持
        if (!StrUtil.equalsAnyIgnoreCase(format, "xlsx", "csv")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的导出格式，仅支持xlsx和csv");
        }

        String fileUrl = categoryService.exportCategories(categoryExportRequest);
        return ResultUtils.success(fileUrl);
    }

    // 前台接口

    @ApiOperation("获取分类列表(不分页)")
    @GetMapping("/category/list")
    public Result<List<CategoryVO>> listCategoriesByType(@ApiParam("分类类型") @RequestParam(required = false) String type) {
        // 参数校验
        if (StrUtil.isNotBlank(type) && type.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类类型长度不能超过20个字符");
        }

        List<CategoryVO> categoryList = categoryService.listCategoriesByType(type);
        return ResultUtils.success(categoryList);
    }

    @ApiOperation("获取分类树形结构(前台)")
    @GetMapping("/category/frontend-tree")
    public Result<List<CategoryTreeVO>> getCategoryTreeForFrontend(@ApiParam("分类类型") @RequestParam(required = false) String type) {
        // 参数校验
        if (StrUtil.isNotBlank(type) && type.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类类型长度不能超过20个字符");
        }

        List<CategoryTreeVO> categoryTree = categoryService.getCategoryTree(type);
        return ResultUtils.success(categoryTree);
    }
}