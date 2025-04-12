package org.leocoder.picture.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.domain.dto.tag.*;
import org.leocoder.picture.domain.vo.tag.*;
import org.leocoder.picture.enums.TagStatusEnum;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.service.TagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 11:40
 * @description : 标签管理相关接口
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "标签管理")
public class TagController {

    private final TagService tagService;

    // 标签颜色正则：HEX格式，如 #1890FF
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");

    @ApiOperation("获取标签统计信息")
    @GetMapping("/admin/tag/statistics")
    public Result<TagStatisticsVO> getTagStatistics() {
        TagStatisticsVO statistics = tagService.getTagStatistics();
        return ResultUtils.success(statistics);
    }

    @ApiOperation("分页获取标签列表")
    @GetMapping("/admin/tag/list/page")
    public Result<PageResult<TagVO>> listTagByPage(TagQueryRequest queryRequest) {
        // 检查查询请求是否为空
        if (ObjectUtil.isNull(queryRequest)) {
            queryRequest = new TagQueryRequest();
        }

        // 校验分页参数
        Integer pageNum = queryRequest.getPageNum();
        Integer pageSize = queryRequest.getPageSize();
        if (ObjectUtil.isNotEmpty(pageNum) && pageNum <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }
        if (ObjectUtil.isNotEmpty(pageSize) && (pageSize <= 0 || pageSize > 100)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页条数必须在1-100之间");
        }

        // 校验状态参数
        if (StrUtil.isNotBlank(queryRequest.getStatus()) &&
                !TagStatusEnum.isValid(queryRequest.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签状态值无效");
        }

        PageResult<TagVO> pageResult = tagService.listTagByPage(queryRequest);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("获取标签详情")
    @GetMapping("/admin/tag/get")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "标签ID", required = true, dataType = "Long")
    })
    public Result<TagVO> getTagById(@RequestParam Long id) {
        // 校验标签ID
        if (ObjectUtil.isNull(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID不能为空");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID必须大于0");
        }

        TagVO tagVO = tagService.getTagById(id);
        return ResultUtils.success(tagVO);
    }

    @ApiOperation("创建标签")
    @PostMapping("/admin/tag/create")
    public Result<Long> createTag(@RequestBody TagCreateRequest createRequest) {
        // 校验参数
        if (ObjectUtil.isNull(createRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 校验标签名
        if (StrUtil.isBlank(createRequest.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签名称不能为空");
        }
        if (createRequest.getName().length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签名称长度不能超过30个字符");
        }

        // 校验颜色（可选）
        if (StrUtil.isNotBlank(createRequest.getColor()) &&
                !COLOR_PATTERN.matcher(createRequest.getColor()).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "颜色必须是有效的HEX格式，如 #1890FF");
        }

        // 校验描述（可选）
        if (StrUtil.isNotBlank(createRequest.getDescription()) &&
                createRequest.getDescription().length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签描述长度不能超过100个字符");
        }


        Long tagId = tagService.createTag(createRequest);
        return ResultUtils.success(tagId);
    }

    @ApiOperation("更新标签")
    @PutMapping("/admin/tag/update")
    public Result<Boolean> updateTag(@RequestBody TagUpdateRequest updateRequest) {
        // 校验参数
        if (ObjectUtil.isNull(updateRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 校验ID
        if (ObjectUtil.isNull(updateRequest.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID不能为空");
        }
        if (updateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID必须大于0");
        }

        // 校验标签名
        if (StrUtil.isBlank(updateRequest.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签名称不能为空");
        }
        if (updateRequest.getName().length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签名称长度不能超过30个字符");
        }

        // 校验颜色（可选）
        if (StrUtil.isNotBlank(updateRequest.getColor()) &&
                !COLOR_PATTERN.matcher(updateRequest.getColor()).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "颜色必须是有效的HEX格式，如 #1890FF");
        }

        // 校验描述（可选）
        if (StrUtil.isNotBlank(updateRequest.getDescription()) &&
                updateRequest.getDescription().length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签描述长度不能超过100个字符");
        }


        Boolean result = tagService.updateTag(updateRequest);
        return ResultUtils.success(result);
    }

    @ApiOperation("删除标签")
    @DeleteMapping("/admin/tag/delete")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "标签ID", required = true, dataType = "Long")
    })
    public Result<Boolean> deleteTag(@RequestParam Long id) {
        // 校验标签ID
        if (ObjectUtil.isNull(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID不能为空");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID必须大于0");
        }

        Boolean result = tagService.deleteTag(id);
        return ResultUtils.success(result);
    }

    @ApiOperation("批量删除标签")
    @DeleteMapping("/admin/tag/batch/delete")
    public Result<Boolean> batchDeleteTags(@RequestBody TagBatchDeleteRequest deleteRequest) {
        // 校验参数
        if (ObjectUtil.isNull(deleteRequest) || CollUtil.isEmpty(deleteRequest.getIds())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID列表不能为空");
        }

        // 校验ID列表
        for (Long id : deleteRequest.getIds()) {
            if (id <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID必须大于0");
            }
        }

        Boolean result = tagService.batchDeleteTags(deleteRequest.getIds());
        return ResultUtils.success(result);
    }

    @ApiOperation("更新标签状态")
    @PutMapping("/admin/tag/status")
    public Result<Boolean> updateTagStatus(@RequestBody TagStatusUpdateRequest statusUpdateRequest) {
        // 校验参数
        if (ObjectUtil.isNull(statusUpdateRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 校验ID
        if (ObjectUtil.isNull(statusUpdateRequest.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID不能为空");
        }
        if (statusUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID必须大于0");
        }

        // 校验状态
        if (StrUtil.isBlank(statusUpdateRequest.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态不能为空");
        }
        if (!TagStatusEnum.isValid(statusUpdateRequest.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值无效，只能是 active 或 inactive");
        }

        Boolean result = tagService.updateTagStatus(statusUpdateRequest);
        return ResultUtils.success(result);
    }

    @ApiOperation("批量更新标签状态")
    @PutMapping("/admin/tag/batch/status")
    public Result<Boolean> batchUpdateTagStatus(@RequestBody TagBatchTagStatusUpdateRequest batchStatusUpdateRequest) {
        // 校验参数
        if (ObjectUtil.isNull(batchStatusUpdateRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 校验ID列表
        if (CollUtil.isEmpty(batchStatusUpdateRequest.getIds())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID列表不能为空");
        }
        for (Long id : batchStatusUpdateRequest.getIds()) {
            if (id <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID必须大于0");
            }
        }

        // 校验状态
        if (StrUtil.isBlank(batchStatusUpdateRequest.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态不能为空");
        }
        if (!TagStatusEnum.isValid(batchStatusUpdateRequest.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态值无效，只能是 active 或 inactive");
        }

        Boolean result = tagService.batchUpdateTagStatus(batchStatusUpdateRequest);
        return ResultUtils.success(result);
    }

    @ApiOperation("获取标签关联的内容")
    @GetMapping("/admin/tag/related/items")
    public Result<PageResult<TagRelatedItemVO>> getTagRelatedItems(
            @RequestParam Long tagId,
            @RequestParam(required = false) String contentType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 校验标签ID
        if (ObjectUtil.isNull(tagId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID不能为空");
        }
        if (tagId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID必须大于0");
        }

        // 校验分页参数
        if (pageNum <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }
        if (pageSize <= 0 || pageSize > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页条数必须在1-100之间");
        }

        PageResult<TagRelatedItemVO> pageResult = tagService.getTagRelatedItems(tagId, contentType, pageNum, pageSize);
        return ResultUtils.success(pageResult);
    }

    @ApiOperation("获取标签使用趋势数据")
    @GetMapping("/admin/tag/usage/trend")
    public Result<List<TagUsageTrendVO>> getTagUsageTrend(
            @RequestParam Long tagId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        // 校验标签ID
        if (ObjectUtil.isNull(tagId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID不能为空");
        }
        if (tagId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签ID必须大于0");
        }

        // 校验日期参数
        if (StrUtil.isBlank(startDate)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始日期不能为空");
        }
        if (StrUtil.isBlank(endDate)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束日期不能为空");
        }

        // 简单校验日期格式
        if (!startDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始日期格式不正确，应为yyyy-MM-dd");
        }
        if (!endDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束日期格式不正确，应为yyyy-MM-dd");
        }

        List<TagUsageTrendVO> trendData = tagService.getTagUsageTrend(tagId, startDate, endDate);
        return ResultUtils.success(trendData);
    }

    // 前台接口

    @ApiOperation("获取标签列表（不分页）")
    @GetMapping("/tag/list")
    public Result<List<TagVO>> getTagList() {
        List<TagVO> tagList = tagService.getTagList();
        return ResultUtils.success(tagList);
    }

}