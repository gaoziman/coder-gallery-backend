package org.leocoder.picture.controller;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.domain.dto.picture.PictureEditRequest;
import org.leocoder.picture.domain.dto.picture.PictureUploadByBatchRequest;
import org.leocoder.picture.domain.dto.picture.PictureUploadRequest;
import org.leocoder.picture.domain.dto.picture.PictureWaterfallRequest;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.picture.PictureVO;
import org.leocoder.picture.domain.vo.picture.PictureWaterfallVO;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.exception.ThrowUtils;
import org.leocoder.picture.service.PictureService;
import org.leocoder.picture.utils.UserContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-10 11:40
 * @description : 图片相关接口
 */
@RestController
@RequestMapping("/picture")
@RequiredArgsConstructor
@Api(tags = "图片相关接口")
@Slf4j
public class PictureController {

    private final PictureService pictureService;


    @ApiOperation(value = "上传图片（可重新上传）")
    @PostMapping("/upload")
    public Result<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest requestParam) {
        // 获取登录用户
        User loginUser = UserContext.getUser();
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, requestParam, loginUser);
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/upload/url")
    @ApiOperation(value = "url上传图片")
    public Result<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest requestParam) {
        // 获取登录用户
        User loginUser = UserContext.getUser();
        String fileUrl = requestParam.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, requestParam, loginUser);
        return ResultUtils.success(pictureVO);
    }


    @ApiOperation(value = "获取图片详情")
    @GetMapping("/{id}")
    public Result<PictureVO> getPictureById(@PathVariable("id") Long id) {
        User loginUser = UserContext.getUser();
        PictureVO pictureVO = pictureService.getPictureById(id, loginUser);
        return ResultUtils.success(pictureVO);
    }


    @ApiOperation(value = "获取上一张图片")
    @GetMapping("/{id}/previous")
    public Result<PictureVO> getPreviousPicture(@PathVariable("id") Long id) {
        User loginUser = UserContext.getUser();
        PictureVO pictureVO = pictureService.getPreviousPicture(id, loginUser);
        return ResultUtils.success(pictureVO);
    }

    @ApiOperation(value = "获取下一张图片")
    @GetMapping("/{id}/next")
    public Result<PictureVO> getNextPicture(@PathVariable("id") Long id) {
        User loginUser = UserContext.getUser();
        PictureVO pictureVO = pictureService.getNextPicture(id, loginUser);
        return ResultUtils.success(pictureVO);
    }

    @ApiOperation(value = "批量抓取图片")
    @PostMapping("/upload/batch")
    public Result<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest requestParam) {
        // 校验参数
        ThrowUtils.throwIf(ObjectUtil.isNull(requestParam), ErrorCode.PARAMS_ERROR);
        User loginUser = UserContext.getUser();
        int uploadCount = pictureService.uploadPictureByBatch(requestParam, loginUser);
        return ResultUtils.success(uploadCount);
    }

    @ApiOperation(value = "获取首页瀑布流图片列表")
    @PostMapping("/waterfall")
    public Result<PictureWaterfallVO> getWaterfallPictures(@RequestBody(required = false) PictureWaterfallRequest requestParam) {
        User loginUser = UserContext.getUser();
        PictureWaterfallVO pictureWaterfallVO = pictureService.getWaterfallPictures(requestParam, loginUser);
        return ResultUtils.success(pictureWaterfallVO);
    }


    @ApiOperation(value = "加载更多瀑布流图片")
    @PostMapping("/waterfall/more")
    public Result<PictureWaterfallVO> loadMoreWaterfallPictures(
            @RequestParam("lastId") Long lastId,
            @RequestParam(value = "lastValue", required = false) String lastValueStr,
            @RequestBody(required = false) PictureWaterfallRequest request) {

        User loginUser = UserContext.getUser();

        // 根据排序方式转换lastValue为正确类型
        Object typedLastValue = null;
        if (lastValueStr != null) {
            String sortBy = request != null ? request.getSortBy() : "newest";
            if (sortBy == null) {
                sortBy = "newest";
            }

            try {
                switch (sortBy) {
                    case "newest":
                        // 时间戳转换为LocalDateTime
                        typedLastValue = LocalDateTime.parse(lastValueStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        break;
                    case "popular":
                    case "mostViewed":
                    case "mostLiked":
                    case "mostCollected":
                        // 数值类型转换为Long
                        typedLastValue = Long.parseLong(lastValueStr);
                        break;
                    default:
                        typedLastValue = lastValueStr;
                }
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "lastValue格式不正确: " + e.getMessage());
            }
        }

        PictureWaterfallVO pictureWaterfallVO = pictureService.loadMoreWaterfallPictures(
                lastId, typedLastValue, request, loginUser);
        return ResultUtils.success(pictureWaterfallVO);
    }


    @ApiOperation(value = "编辑图片（用户使用）")
    @PostMapping("/edit")
    public Result<Boolean> editPicture(@RequestBody PictureEditRequest requestParam) {
        // 参数基础校验
        if (ObjectUtil.isNull(requestParam) || requestParam.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 调用服务层方法执行更新操作
        Boolean result = pictureService.editPicture(requestParam);

        // 返回实际的操作结果
        return ResultUtils.success(result);
    }
}

