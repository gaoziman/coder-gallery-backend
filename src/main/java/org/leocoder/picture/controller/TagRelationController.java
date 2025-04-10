package org.leocoder.picture.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.domain.dto.tag.TagRelationBatchCreateRequest;
import org.leocoder.picture.domain.dto.tag.TagRelationUpdateRequest;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.service.TagRelationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 09:32
 * @description : 标签关系管理相关接口
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@Api(tags = "标签关系管理")
public class TagRelationController {

    private final TagRelationService tagRelationService;

    @ApiOperation("创建标签关系")
    @PostMapping("/api/tag/relation/create")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tagId", value = "标签ID", required = true, dataType = "Long"),
            @ApiImplicitParam(name = "contentType", value = "内容类型", required = true, dataType = "String"),
            @ApiImplicitParam(name = "contentId", value = "内容ID", required = true, dataType = "Long")
    })
    public Result<Boolean> createTagRelation(
            @RequestParam Long tagId,
            @RequestParam String contentType,
            @RequestParam Long contentId) {

        // 校验标签ID
        if (ObjectUtil.isNull(tagId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID不能为空");
        }
        if (tagId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID必须大于0");
        }

        // 校验内容类型
        if (StrUtil.isBlank(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }
        if (contentType.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过50个字符");
        }

        // 校验内容ID
        if (ObjectUtil.isNull(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不能为空");
        }
        if (contentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID必须大于0");
        }

        Boolean result = tagRelationService.createTagRelation(tagId, contentType, contentId);
        return ResultUtils.success(result);
    }

    @ApiOperation("批量创建标签关系")
    @PostMapping("/api/tag/relation/batch/create")
    public Result<Boolean> batchCreateTagRelations(@RequestBody TagRelationBatchCreateRequest request) {
        // 校验参数
        if (ObjectUtil.isNull(request)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 校验标签ID列表
        if (CollUtil.isEmpty(request.getTagIds())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID列表不能为空");
        }
        for (Long tagId : request.getTagIds()) {
            if (tagId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID必须大于0");
            }
        }

        // 校验内容类型
        if (StrUtil.isBlank(request.getContentType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }
        if (request.getContentType().length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过50个字符");
        }

        // 校验内容ID
        if (ObjectUtil.isNull(request.getContentId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不能为空");
        }
        if (request.getContentId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID必须大于0");
        }

        Boolean result = tagRelationService.batchCreateTagRelations(
                request.getTagIds(), request.getContentType(), request.getContentId());
        return ResultUtils.success(result);
    }

    @ApiOperation("删除标签关系")
    @DeleteMapping("/api/tag/relation/delete")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tagId", value = "标签ID", required = true, dataType = "Long"),
            @ApiImplicitParam(name = "contentType", value = "内容类型", required = true, dataType = "String"),
            @ApiImplicitParam(name = "contentId", value = "内容ID", required = true, dataType = "Long")
    })
    public Result<Boolean> deleteTagRelation(
            @RequestParam Long tagId,
            @RequestParam String contentType,
            @RequestParam Long contentId) {

        // 校验标签ID
        if (ObjectUtil.isNull(tagId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID不能为空");
        }
        if (tagId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID必须大于0");
        }

        // 校验内容类型
        if (StrUtil.isBlank(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }
        if (contentType.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过50个字符");
        }

        // 校验内容ID
        if (ObjectUtil.isNull(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不能为空");
        }
        if (contentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID必须大于0");
        }

        Boolean result = tagRelationService.deleteTagRelation(tagId, contentType, contentId);
        return ResultUtils.success(result);
    }

    @ApiOperation("删除内容的所有标签关系")
    @DeleteMapping("/api/tag/relation/content/delete")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "contentType", value = "内容类型", required = true, dataType = "String"),
            @ApiImplicitParam(name = "contentId", value = "内容ID", required = true, dataType = "Long")
    })
    public Result<Boolean> deleteAllRelationsByContent(
            @RequestParam String contentType,
            @RequestParam Long contentId) {

        // 校验内容类型
        if (StrUtil.isBlank(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }
        if (contentType.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过50个字符");
        }

        // 校验内容ID
        if (ObjectUtil.isNull(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不能为空");
        }
        if (contentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID必须大于0");
        }

        Boolean result = tagRelationService.deleteAllRelationsByContent(contentType, contentId);
        return ResultUtils.success(result);
    }

    @ApiOperation("更新内容标签关系")
    @PutMapping("/api/tag/relation/update")
    public Result<Boolean> updateContentTags(@RequestBody TagRelationUpdateRequest request) {
        // 校验参数
        if (ObjectUtil.isNull(request)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 校验内容类型
        if (StrUtil.isBlank(request.getContentType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }
        if (request.getContentType().length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过50个字符");
        }

        // 校验内容ID
        if (ObjectUtil.isNull(request.getContentId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不能为空");
        }
        if (request.getContentId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID必须大于0");
        }

        // 校验标签ID列表（可选）
        if (CollUtil.isNotEmpty(request.getTagIds())) {
            for (Long tagId : request.getTagIds()) {
                if (tagId <= 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID必须大于0");
                }
            }
        }

        Boolean result = tagRelationService.updateContentTags(
                request.getTagIds(), request.getContentType(), request.getContentId());
        return ResultUtils.success(result);
    }

    @ApiOperation("获取内容关联的标签ID列表")
    @GetMapping("/api/tag/relation/list")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "contentType", value = "内容类型", required = true, dataType = "String"),
            @ApiImplicitParam(name = "contentId", value = "内容ID", required = true, dataType = "Long")
    })
    public Result<List<Long>> getTagIdsByContent(
            @RequestParam String contentType,
            @RequestParam Long contentId) {

        // 校验内容类型
        if (StrUtil.isBlank(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }
        if (contentType.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过50个字符");
        }

        // 校验内容ID
        if (ObjectUtil.isNull(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不能为空");
        }
        if (contentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID必须大于0");
        }

        List<Long> tagIds = tagRelationService.getTagIdsByContent(contentType, contentId);
        return ResultUtils.success(tagIds);
    }

    @ApiOperation("检查标签关系是否存在")
    @GetMapping("/api/tag/relation/check")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "tagId", value = "标签ID", required = true, dataType = "Long"),
            @ApiImplicitParam(name = "contentType", value = "内容类型", required = true, dataType = "String"),
            @ApiImplicitParam(name = "contentId", value = "内容ID", required = true, dataType = "Long")
    })
    public Result<Boolean> checkRelationExists(
            @RequestParam Long tagId,
            @RequestParam String contentType,
            @RequestParam Long contentId) {

        // 校验标签ID
        if (ObjectUtil.isNull(tagId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID不能为空");
        }
        if (tagId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID必须大于0");
        }

        // 校验内容类型
        if (StrUtil.isBlank(contentType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型不能为空");
        }
        if (contentType.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容类型长度不能超过50个字符");
        }

        // 校验内容ID
        if (ObjectUtil.isNull(contentId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID不能为空");
        }
        if (contentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容ID必须大于0");
        }

        Boolean exists = tagRelationService.checkRelationExists(tagId, contentType, contentId);
        return ResultUtils.success(exists);
    }
}