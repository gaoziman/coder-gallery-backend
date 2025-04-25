package org.leocoder.picture.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.config.CosClientConfig;
import org.leocoder.picture.domain.dto.upload.UploadPictureResult;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.manager.CosManager;

import javax.annotation.Resource;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 00:19
 * @description : 图片上传模板 抽象类
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    protected CosManager cosManager;

    @Resource
    protected CosClientConfig cosClientConfig;

    /**
     * 模板方法，定义上传流程
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片
        validPicture(inputSource);

        // 2. 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        File file = null;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源（本地或 URL）
            processFile(inputSource, file);

            // 4. 上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObjectPublicRead(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                CIObject compressedCiObject = objectList.get(0);
                // 缩略图默认等于压缩图
                CIObject thumbnailCiObject = compressedCiObject;
                // 有生成缩略图，才得到缩略图
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                // 封装压缩图返回结果
                return buildResult(originFilename, compressedCiObject, thumbnailCiObject, imageInfo);
            }

            // 5. 封装返回结果
            return buildResult(originFilename, file, uploadPath, imageInfo);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败: " + e.getMessage());
        } finally {
            // 6. 清理临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 校验输入源（本地文件或 URL）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 将颜色值转换为十六进制格式 (#RRGGBB)
     *
     * @param colorValue 原始颜色值
     * @return 十六进制格式的颜色值
     */
    private String convertToHexColor(String colorValue) {
        if (StrUtil.isBlank(colorValue)) {
            return "#000000";
        }

        // 如果已经是十六进制格式，直接返回
        if (colorValue.startsWith("#") && colorValue.length() == 7) {
            return colorValue;
        }

        try {
            // 尝试解析 RGB 格式 (r,g,b)
            Pattern rgbPattern = Pattern.compile("\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*");
            Matcher matcher = rgbPattern.matcher(colorValue);

            if (matcher.matches()) {
                int r = Integer.parseInt(matcher.group(1));
                int g = Integer.parseInt(matcher.group(2));
                int b = Integer.parseInt(matcher.group(3));

                // 确保颜色值在有效范围内
                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));

                // 转换为十六进制格式
                return String.format("#%02X%02X%02X", r, g, b);
            }

            // 尝试直接解析数字（可能是RGB整数值）
            if (colorValue.matches("\\d+")) {
                int rgb = Integer.parseInt(colorValue);
                Color color = new Color(rgb);
                return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
            }

            // 如果是其他格式，记录日志并尝试返回一个合理的默认值
            log.warn("无法解析颜色值: {}, 使用默认颜色", colorValue);
            return "#C41D7F";
        } catch (Exception e) {
            log.error("颜色转换异常: {}, 原始值: {}", e.getMessage(), colorValue);
            return "#C41D7F";
        }
    }

    /**
     * 封装返回结果
     *
     * @param originFilename 原始文件名
     * @param file           临时文件
     * @param uploadPath     上传路径
     * @param imageInfo      图片信息
     * @return 上传结果
     */
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setOriginalName(originFilename);
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        // 将颜色值转换为十六进制格式
        uploadPictureResult.setPicColor(convertToHexColor(imageInfo.getAve()));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        return uploadPictureResult;
    }

    /**
     * 封装返回结果
     *
     * @param originFilename     原始文件名
     * @param compressedCiObject 压缩后的图片对象
     * @param thumbnailCiObject  缩略图对象
     * @param imageInfo          图片信息
     * @return 上传结果
     */
    private UploadPictureResult buildResult(String originFilename, CIObject compressedCiObject, CIObject thumbnailCiObject, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setOriginalName(originFilename);
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        // 将颜色值转换为十六进制格式
        uploadPictureResult.setPicColor(convertToHexColor(imageInfo.getAve()));
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        // 设置图片为压缩后的地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        // 设置缩略图
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }

    /**
     * 批量验证和上传图片URL
     * @param imageUrls 图片URL列表
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传成功的结果列表
     */
    public List<UploadPictureResult> batchValidateAndUpload(List<String> imageUrls, String uploadPathPrefix) {
        List<UploadPictureResult> results = new ArrayList<>();

        for (String imageUrl : imageUrls) {
            try {
                // 验证URL
                validPicture(imageUrl);

                // 上传图片
                UploadPictureResult result = uploadPicture(imageUrl, uploadPathPrefix);
                results.add(result);
                log.info("成功验证并上传图片: {}", imageUrl);
            } catch (Exception e) {
                log.warn("图片URL验证或上传失败: {}, 错误: {}", imageUrl, e.getMessage());
            }
        }

        return results;
    }
}