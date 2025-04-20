package org.leocoder.picture.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date  2025-04-11 00:20
 * @description : 上传配置类
 */

@Component
@ConfigurationProperties(prefix = "upload")
@Data
public class UploadProperties {
    // 最大文件大小，单位字节，默认6MB
    private long maxSize = 6 * 1024 * 1024L;
    
    // 允许的文件扩展名
    private List<String> allowedExtensions = Arrays.asList("jpeg", "jpg", "png", "webp", "heic");
    
    // 允许的内容类型
    private List<String> allowedContentTypes = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp", "image/heic");

}