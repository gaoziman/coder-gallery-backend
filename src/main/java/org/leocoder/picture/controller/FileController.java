package org.leocoder.picture.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.config.CosClientConfig;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.manager.CosManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 00:21
 * @description : 文件上传控制器
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/file")
@Api(tags = "文件上传")
public class FileController {

    private final CosManager cosManager;
    private final CosClientConfig cosClientConfig;

    // 配置文件中定义这些参数，使其可配置
    @Value("${upload.max-size:6291456}") // 默认6MB = 6 * 1024 * 1024
    private long maxFileSize;

    @Value("${upload.allowed-extensions:jpeg,jpg,png,webp,heic}")
    private String allowedExtensionsString;

    private Set<String> allowedExtensions;

    /**
     * 初始化允许的文件扩展名集合
     */
    @PostConstruct
    public void init() {
        allowedExtensions = new HashSet<>(Arrays.asList(allowedExtensionsString.split(",")));
        log.info("初始化文件上传配置 - 最大文件大小: {}字节, 允许的扩展名: {}", maxFileSize, allowedExtensionsString);
    }

    /**
     * 上传图片
     *
     * @param file 图片文件
     * @return 图片URL
     */
    @PostMapping(value = "/upload")
    @ApiOperation(value = "图片上传", notes = "上传图片并返回访问URL")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        // 1. 验证文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "文件不能为空");
        }

        // 2. 验证文件大小
        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR,
                    String.format("文件大小不能超过%dMB", maxFileSize / (1024 * 1024)));
        }

        // 3. 获取文件名和扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.lastIndexOf(".") == -1) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "无效的文件名");
        }

        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        // 4. 验证文件类型
        if (!allowedExtensions.contains(suffix)) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR,
                    "不支持的文件类型，仅支持: " + allowedExtensionsString);
        }

        try {
            // 生成文件名
            String fileName = UUID.randomUUID().toString().replaceAll("-", "") + "." + suffix;
            String uploadPath = "images/" + fileName;

            // 创建临时文件
            File tempFile = File.createTempFile("temp_", "." + suffix);
            file.transferTo(tempFile);

            // 上传到腾讯云COS
            log.info("开始上传图片: {}", uploadPath);
            cosManager.putObjectPublicRead(uploadPath, tempFile, file.getContentType());

            // 生成访问URL
            String imageUrl = cosClientConfig.getHost() + "/" + uploadPath;

            // 删除临时文件
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
                }
            }

            log.info("图片上传成功: {}", imageUrl);
            return ResultUtils.success(imageUrl);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "图片上传失败: " + e.getMessage());
        }
    }
}