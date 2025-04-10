package org.leocoder.picture.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.leocoder.picture.annotation.Log;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.service.CategoryRelationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 分类关系相关接口
 */
@RestController
@RequestMapping("/category_relation")
@RequiredArgsConstructor
@Api(tags = "分类关系管理")
public class CategoryRelationController {

    private final CategoryRelationService categoryRelationService;

    @ApiOperation("创建分类关系")
    @PostMapping("/create")
    @Log(module = "分类关联管理", action = "创建分类关系")
    public Result<Boolean> createCategoryRelation(
            @ApiParam("分类ID") @RequestParam Long categoryId,
            @ApiParam("内容类型") @RequestParam String contentType,
            @ApiParam("内容ID") @RequestParam Long contentId) {

        // 参数校验
        if (ObjectUtil.isEmpty(categoryId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }
        if (categoryId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID必须大于0");
        }

        if (StrUtil.isBlank(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }
        if (contentType.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过20个字符");
        }

        if (ObjectUtil.isEmpty(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不能为空");
        }
        if (contentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID必须大于0");
        }

        Boolean result = categoryRelationService.createCategoryRelation(categoryId, contentType, contentId);
        return ResultUtils.success(result);
    }

    @ApiOperation("批量创建分类关系")
    @PostMapping("/batch/create")
    @Log(module = "分类关联管理", action = "批量创建分类关系")
    public Result<Boolean> batchCreateCategoryRelations(
            @ApiParam("分类ID列表") @RequestParam List<Long> categoryIds,
            @ApiParam("内容类型") @RequestParam String contentType,
            @ApiParam("内容ID") @RequestParam Long contentId) {

        // 参数校验
        if (CollUtil.isEmpty(categoryIds)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID列表不能为空");
        }

        for (Long categoryId : categoryIds) {
            if (categoryId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID必须大于0");
            }
        }

        if (StrUtil.isBlank(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }
        if (contentType.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过20个字符");
        }

        if (ObjectUtil.isEmpty(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不能为空");
        }
        if (contentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID必须大于0");
        }

        Boolean result = categoryRelationService.batchCreateCategoryRelations(categoryIds, contentType, contentId);
        return ResultUtils.success(result);
    }

    @ApiOperation("删除分类关系")
    @DeleteMapping("/delete")
    @Log(module = "分类关联管理", action = "删除分类关系")
    public Result<Boolean> deleteCategoryRelation(
            @ApiParam("分类ID") @RequestParam Long categoryId,
            @ApiParam("内容类型") @RequestParam String contentType,
            @ApiParam("内容ID") @RequestParam Long contentId) {

        // 参数校验
        if (ObjectUtil.isEmpty(categoryId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }
        if (categoryId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID必须大于0");
        }

        if (StrUtil.isBlank(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }
        if (contentType.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过20个字符");
        }

        if (ObjectUtil.isEmpty(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不能为空");
        }
        if (contentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID必须大于0");
        }

        Boolean result = categoryRelationService.deleteCategoryRelation(categoryId, contentType, contentId);
        return ResultUtils.success(result);
    }

    @ApiOperation("删除指定内容的所有分类关系")
    @DeleteMapping("/delete/by-content")
    @Log(module = "分类关联管理", action = "删除指定内容的所有分类关系")
    public Result<Boolean> deleteAllRelationsByContent(
            @ApiParam("内容类型") @RequestParam String contentType,
            @ApiParam("内容ID") @RequestParam Long contentId) {

        // 参数校验
        if (StrUtil.isBlank(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }
        if (contentType.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过20个字符");
        }

        if (ObjectUtil.isEmpty(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不能为空");
        }
        if (contentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID必须大于0");
        }

        Boolean result = categoryRelationService.deleteAllRelationsByContent(contentType, contentId);
        return ResultUtils.success(result);
    }

    @ApiOperation("更新内容的分类关系")
    @PutMapping("/update")
    @Log(module = "分类关联管理", action = "更新内容的分类关系")
    public Result<Boolean> updateContentCategories(
            @ApiParam("分类ID列表") @RequestParam List<Long> categoryIds,
            @ApiParam("内容类型") @RequestParam String contentType,
            @ApiParam("内容ID") @RequestParam Long contentId) {

        // 参数校验
        if (categoryIds == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID列表不能为null");
        }

        // 注意：这里允许空列表，表示清空所有分类关系
        for (Long categoryId : categoryIds) {
            if (categoryId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID必须大于0");
            }
        }

        if (StrUtil.isBlank(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }
        if (contentType.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过20个字符");
        }

        if (ObjectUtil.isEmpty(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不能为空");
        }
        if (contentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID必须大于0");
        }

        Boolean result = categoryRelationService.updateContentCategories(categoryIds, contentType, contentId);
        return ResultUtils.success(result);
    }

    @ApiOperation("获取内容关联的分类ID列表")
    @GetMapping("/get-category-ids")
    @Log(module = "分类关联管理", action = "获取内容关联的分类ID列表")
    public Result<List<Long>> getCategoryIdsByContent(
            @ApiParam("内容类型") @RequestParam String contentType,
            @ApiParam("内容ID") @RequestParam Long contentId) {

        // 参数校验
        if (StrUtil.isBlank(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }
        if (contentType.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过20个字符");
        }

        if (ObjectUtil.isEmpty(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不能为空");
        }
        if (contentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID必须大于0");
        }

        List<Long> categoryIds = categoryRelationService.getCategoryIdsByContent(contentType, contentId);
        return ResultUtils.success(categoryIds);
    }

    @ApiOperation("查询分类下有多少内容")
    @GetMapping("/count")
    @Log(module = "分类关联管理", action = "查询分类下有多少内容")
    public Result<Integer> countContentsByCategory(
            @ApiParam("分类ID") @RequestParam Long categoryId,
            @ApiParam("内容类型") @RequestParam(required = false) String contentType) {

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

        Integer count = categoryRelationService.countContentsByCategory(categoryId, contentType);
        return ResultUtils.success(count);
    }
}