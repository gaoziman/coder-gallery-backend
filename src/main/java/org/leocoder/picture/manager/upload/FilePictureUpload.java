package org.leocoder.picture.manager.upload;

import cn.hutool.core.io.FileUtil;
import lombok.RequiredArgsConstructor;
import org.leocoder.picture.config.UploadProperties;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-11 00:20
 * @description : 文件上传子类实现
 */
@Service
@RequiredArgsConstructor
public class FilePictureUpload extends PictureUploadTemplate {

    private final UploadProperties uploadProperties;

    /**
     * 校验上传文件
     *
     * @param inputSource 上传文件对象
     */
    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.FILE_NOT_NULL);
        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = uploadProperties.getMaxSize();
        ThrowUtils.throwIf(fileSize >  ONE_M, ErrorCode.FILE_SIZE_ERROR);

        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());

        // 允许上传的文件后缀
        final List<String> ALLOW_FORMAT_LIST = uploadProperties.getAllowedExtensions();
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.FILE_TYPE_ERROR);
    }

    /**
     * 获取原始文件名
     *
     * @param inputSource 上传文件对象
     * @return 原始文件名
     */
    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }


    /**
     * 处理上传文件
     *
     * @param inputSource 上传文件对象
     * @param file        上传文件
     * @throws Exception 异常
     */
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}

